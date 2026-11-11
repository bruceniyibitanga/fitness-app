package app.levitx.phone.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.coach.bodyweightTrend
import app.levitx.core.load.LoadLimits
import app.levitx.core.session.SetLoad
import app.levitx.data.coach.CoachRepository
import app.levitx.data.coach.WorkoutDraftRepository
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.time.TrainingClock
import app.levitx.phone.handlingDataFailures
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs the workout in progress: shows each set, records what the person did,
 * times the rest, and hands the finished workout to the coach.
 *
 * The rules for moving through a workout are [WorkoutRun]'s. This class only
 * connects them to the screen and keeps them safe: after every change the run
 * is saved as the draft, so a workout survives the app being killed.
 */
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val coachRepository: CoachRepository,
    private val drafts: WorkoutDraftRepository,
    private val equipmentRepository: EquipmentRepository,
    private val clock: TrainingClock,
) : ViewModel() {
    private val state = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Loading)

    /** The current screen state. */
    val uiState: StateFlow<WorkoutUiState> = state.asStateFlow()

    /** Draft writes go through this one at a time, in order, so an older run can't overwrite a newer one. */
    private val writes = Mutex()

    init {
        viewModelScope.launch {
            handlingDataFailures(onFailure = { state.value = WorkoutUiState.Unavailable }) {
                val run = drafts.observeDraft().first()
                val coach = coachRepository.observeState().first()
                if (run == null || run.isComplete || coach == null) {
                    state.value = WorkoutUiState.NoWorkout
                } else {
                    // Read once, so every set in the workout uses the same figure.
                    val bodyweight = bodyweightTrend(coach.model.weighIns, clock.currentTimeMillis())?.currentKg
                    state.value = WorkoutUiState.Active(run, bodyweight, equipmentRepository.observeBands().first())
                }
            }
        }
    }

    /** Milliseconds on the rest clock, for the screen's countdown. */
    fun elapsedRealtimeMillis(): Long = clock.elapsedRealtimeMillis()

    /**
     * Records the set just done. Starts rest, or saves the workout if it was the last set.
     *
     * @param value Reps or seconds done.
     * @param repsInReserve Reps they felt they had left, or null for holds.
     * @param addedKg Vest or belt weight, 0 for none.
     * @param assistanceKg Band help, 0 for none.
     */
    fun logSet(value: Int, repsInReserve: Int?, addedKg: Double = 0.0, assistanceKg: Double = 0.0) {
        val active = state.value as? WorkoutUiState.Active ?: return
        val next = active.run
            .withLoad(SetLoad(addedKg = addedKg.asModifierKg(), assistanceKg = assistanceKg.asModifierKg()))
            .logSet(value, repsInReserve, nowElapsedMillis = clock.elapsedRealtimeMillis())
        // A run that didn't change means the set wasn't accepted (during rest, say).
        if (next.logged == active.run.logged) return
        advance(active, next)
    }

    /**
     * Sets the vest, belt or band for the sets to come. Kept in the run, so it
     * survives a timer, the app being closed, and carries to the next set.
     */
    fun chooseLoad(addedKg: Double, assistanceKg: Double) {
        val active = state.value as? WorkoutUiState.Active ?: return
        advance(active, active.run.withLoad(SetLoad(addedKg.asModifierKg(), assistanceKg.asModifierKg())))
    }

    /**
     * Ends rest, makes it 15 seconds longer or shorter, or takes back the set
     * that started it. Rest never ends on its own: when it runs out the screen
     * says so and counts the extra, and the person starts the next set.
     */
    fun rest(action: RestAction) {
        val active = state.value as? WorkoutUiState.Active ?: return
        val now = clock.elapsedRealtimeMillis()
        val run = active.run
        advance(
            active,
            when (action) {
                RestAction.END -> run.endRest()
                RestAction.LONGER -> run.adjustRest(REST_STEP_MILLIS, now)
                RestAction.SHORTER -> run.adjustRest(-REST_STEP_MILLIS, now)
                RestAction.UNDO -> run.undoLastSet()
            },
        )
    }

    /**
     * Starts, stops or cancels the timer for a hold or slow negatives. Stopping
     * keeps what it counted as the figure to log, which the person can correct:
     * a phone on the floor can't tell when they let go.
     */
    fun timer(action: TimerAction) {
        val active = state.value as? WorkoutUiState.Active ?: return
        val now = clock.elapsedRealtimeMillis()
        val running = active.timer
        state.value = when (action) {
            TimerAction.START -> active.timerKind?.let { kind ->
                active.copy(timer = newTimer(active, kind, now), counted = null)
            } ?: active

            TimerAction.NOW -> active.copy(
                timer = running?.copy(startsAtElapsedMillis = minOf(running.startsAtElapsedMillis, now)),
            )

            TimerAction.STOP -> if (running ==
                null
            ) {
                active
            } else {
                active.copy(timer = null, counted = running.countAt(now))
            }

            TimerAction.CANCEL -> active.copy(timer = null, counted = null)
        }
    }

    /** Turns timer beeps on or off for the rest of this workout. */
    fun toggleSounds() {
        val active = state.value as? WorkoutUiState.Active ?: return
        state.value = active.copy(soundsOn = !active.soundsOn)
    }

    /** Moves on to the next exercise. Skipped if nothing was done, ended early if some was. */
    fun skipExercise() {
        val active = state.value as? WorkoutUiState.Active ?: return
        advance(active, active.run.skipExercise())
    }

    /** Ends the workout now and saves what was done. With nothing done, it is thrown away. */
    fun finish() {
        val active = state.value as? WorkoutUiState.Active ?: return
        finish(active.run, active.bodyweightKg)
    }

    private fun advance(active: WorkoutUiState.Active, next: WorkoutRun) {
        if (next == active.run) return
        if (next.isComplete) {
            finish(next, active.bodyweightKg)
        } else {
            // A timer belongs to one set, so any change to the run ends it.
            state.value = active.copy(run = next, timer = null, counted = null)
            viewModelScope.launch {
                writes.withLock {
                    handlingDataFailures(onFailure = { state.value = WorkoutUiState.Unavailable }) { drafts.save(next) }
                }
            }
        }
    }

    private fun finish(run: WorkoutRun, bodyweightKg: Double?) {
        val result = run.result(finishedAtEpochMillis = clock.currentTimeMillis())
        // Shown straight away, so a second tap can't save the workout twice.
        state.value = WorkoutUiState.Saving
        viewModelScope.launch {
            // After every draft write already queued, so none of them can bring the draft back.
            writes.withLock {
                handlingDataFailures(onFailure = { state.value = WorkoutUiState.Unavailable }) {
                    if (result == null) {
                        drafts.discard()
                        state.value = WorkoutUiState.NoWorkout
                    } else {
                        val before = coachRepository.observeState().first()?.model
                        coachRepository.recordWorkout(run.id, result)
                        val after = coachRepository.observeState().first()?.model
                        state.value = if (before == null || after == null) {
                            WorkoutUiState.NoWorkout
                        } else {
                            WorkoutUiState.Finished(sessionReport(before, after, result, bodyweightKg))
                        }
                    }
                }
            }
        }
    }
}

/** A timer for the set up next, starting after the lead-in. */
private fun newTimer(active: WorkoutUiState.Active, kind: TimerKind, now: Long): SetTimer = SetTimer(
    kind = kind,
    startsAtElapsedMillis = now + LEAD_IN_MILLIS,
    lowerMillis = (active.plan.exercise.lowerSeconds ?: 0) * MILLIS_PER_SECOND,
    reps = active.nextSet.target,
)

/** Rest moves in 15 second steps, the step people reach for between sets. */
private const val REST_STEP_MILLIS = 15_000L
private const val MILLIS_PER_SECOND = 1_000L

/** A vest, belt or band figure the load rules accept: never negative, never above the limit, never NaN. */
private fun Double.asModifierKg(): Double = if (isFinite()) coerceIn(0.0, LoadLimits.MAX_MODIFIER_KG) else 0.0
