package app.levitx.phone.intervals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.intervals.IntervalRun
import app.levitx.core.intervals.IntervalTimeline
import app.levitx.data.ApplicationScope
import app.levitx.data.session.SessionRepository
import app.levitx.data.settings.AppSettings
import app.levitx.data.settings.SettingsRepository
import app.levitx.data.time.TrainingClock
import app.levitx.phone.handlingDataFailures
import app.levitx.phone.live.LiveIntervals
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How often the clock is checked. Five times a second keeps each beep within a fifth of a second. */
internal const val INTERVAL_TICK_MILLIS = 200L

private const val MILLIS_PER_SECOND = 1_000L

/** Extra time the wake lock is held past the workout's end, in case the last tick runs late. */
private const val AWAKE_MARGIN_MILLIS = 5_000L

/**
 * Runs a cardio or mobility workout: counts each step down against the
 * clock that ignores time-zone and clock changes, beeps and buzzes as steps
 * change, and saves the work done to the history when it ends.
 *
 * While it runs, the workout service keeps the app going with the phone
 * locked or another app in front, as it does for strength workouts.
 *
 * The timer lives as long as this screen. It doesn't survive the app being
 * killed in the background: the run holds catalogue moves, and saved state
 * is written to disk unencrypted, so a half-done workout isn't kept there.
 * Whatever was done is saved when the screen goes away for any reason.
 *
 * Nothing here touches the coach. These workouts never change the plan or
 * count toward the week's hard sets.
 */
@HiltViewModel(assistedFactory = IntervalTimerViewModel.Factory::class)
class IntervalTimerViewModel @AssistedInject constructor(
    @Assisted workoutId: String,
    private val sessions: SessionRepository,
    settings: SettingsRepository,
    private val clock: TrainingClock,
    private val signals: IntervalSignals,
    @param:ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {
    private val workout = IntervalCatalogue.byId(workoutId)

    // A random id, like strength workouts, so the history groups the moves as one workout.
    private val id = UUID.randomUUID().toString()
    private val startedAtEpochMillis = clock.currentTimeMillis()

    private val state = MutableStateFlow(
        workout?.let { IntervalTimerUiState.Running(it, IntervalRun(IntervalTimeline.of(it))) }
            ?: IntervalTimerUiState.NotFound,
    )

    /** The current screen state. */
    val uiState: StateFlow<IntervalTimerUiState> = state.asStateFlow()

    // Settings live in DataStore. If they can't be read, cues play with the defaults.
    private val cueSettings: StateFlow<AppSettings> = settings.observe()
        .catch { emit(AppSettings()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    init {
        (state.value as? IntervalTimerUiState.Running)?.let { running ->
            holdAwake(running.run)
            show(running)
            viewModelScope.launch { runClock() }
        }
    }

    /** Stops or restarts the clock. */
    fun togglePause() {
        val running = state.value as? IntervalTimerUiState.Running ?: return
        val run = running.run.togglePause()
        state.value = running.copy(run = run)
        show(running.copy(run = run))
        if (run.paused) signals.letSleep() else holdAwake(run)
    }

    /** Ends the step under way now. */
    fun skip() = move { it.skip() }

    /** Shows or hides the "End now?" sheet. */
    fun askToEnd(asking: Boolean) {
        state.update { (it as? IntervalTimerUiState.Running)?.copy(askingToEnd = asking) ?: it }
    }

    /** Ends the workout now, keeping what was done. */
    fun end() {
        (state.value as? IntervalTimerUiState.Running)?.let { finish(it.run) }
    }

    override fun onCleared() {
        // Leaving mid-workout still keeps the work done.
        end()
        signals.release()
    }

    /** Ticks the run forward by whole seconds of the elapsed clock until it stops running. */
    private suspend fun runClock() {
        var anchor = clock.elapsedRealtimeMillis()
        while (state.value is IntervalTimerUiState.Running) {
            delay(INTERVAL_TICK_MILLIS)
            val now = clock.elapsedRealtimeMillis()
            val run = (state.value as? IntervalTimerUiState.Running)?.run
            when {
                run == null -> Unit

                // Time spent paused never counts.
                run.paused -> anchor = now

                else -> {
                    val seconds = ((now - anchor) / MILLIS_PER_SECOND).toInt()
                    anchor += seconds * MILLIS_PER_SECOND
                    if (seconds > 0) move { it.tick(seconds) }
                }
            }
        }
    }

    /** Applies [change], plays its cue, and finishes when the run is done. */
    private fun move(change: (IntervalRun) -> IntervalRun) {
        val running = state.value as? IntervalTimerUiState.Running ?: return
        val run = change(running.run)
        intervalCue(running.run, run)?.let { cue -> signals.play(cue, cueSettings.value) }
        if (run.isFinished) {
            finish(run)
        } else {
            state.value = running.copy(run = run)
            if (run.index != running.run.index) {
                holdAwake(run)
                show(running.copy(run = run))
            }
        }
    }

    /** Shows the done screen and saves the work, if any, to the history. */
    private fun finish(run: IntervalRun) {
        val workout = workout ?: return
        signals.letSleep()
        signals.hide()
        val done = run.sessions(startedAtEpochMillis, clock.currentTimeMillis())
        val workedSeconds = done.sumOf { session -> session.sets.sumOf { it.value } }
        state.value = IntervalTimerUiState.Done(
            workout = workout,
            workedSeconds = workedSeconds,
            save = if (done.isEmpty()) SaveStatus.NOTHING else SaveStatus.SAVING,
        )
        if (done.isNotEmpty()) {
            // The app's scope, so leaving the screen doesn't cancel the save.
            appScope.launch {
                val saved = handlingDataFailures(onFailure = { false }) {
                    sessions.saveIntervalWorkout(id, done)
                    true
                }
                state.update { current ->
                    (current as? IntervalTimerUiState.Done)
                        ?.copy(save = if (saved) SaveStatus.SAVED else SaveStatus.FAILED)
                        ?: current
                }
            }
        }
    }

    /** Shows the step [running] is on as the workout in progress. Called once per step, not once per second. */
    private fun show(running: IntervalTimerUiState.Running) {
        val step = running.run.current ?: return
        signals.show(
            LiveIntervals(
                workoutName = running.workout.name,
                phase = step.phase,
                moveName = step.move.name,
                round = step.round,
                rounds = step.rounds,
                paused = running.run.paused,
            ),
        )
    }

    /** Keeps the processor on until [run] would end, so beeps come on time with the screen off. */
    private fun holdAwake(run: IntervalRun) {
        signals.stayAwakeFor(run.totalRemaining * MILLIS_PER_SECOND + AWAKE_MARGIN_MILLIS)
    }

    /** Makes an [IntervalTimerViewModel] for one interval workout. */
    @AssistedFactory
    interface Factory {
        /** The ViewModel for the workout with [workoutId]. */
        fun create(workoutId: String): IntervalTimerViewModel
    }
}
