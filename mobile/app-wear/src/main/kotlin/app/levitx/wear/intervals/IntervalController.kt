package app.levitx.wear.intervals

import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.intervals.IntervalCue
import app.levitx.core.intervals.IntervalRun
import app.levitx.core.intervals.IntervalTimeline
import app.levitx.core.intervals.IntervalWorkout
import app.levitx.core.intervals.cueFrom
import app.levitx.data.ApplicationScope
import app.levitx.data.DataUnavailableException
import app.levitx.data.intervals.IntervalOutbox
import app.levitx.data.intervals.QueuedIntervalWorkout
import app.levitx.data.time.TrainingClock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** How often the clock is checked. Five times a second keeps each buzz within a fifth of a second. */
internal const val INTERVAL_TICK_MILLIS = 200L

private const val MILLIS_PER_SECOND = 1_000L

/** Extra time the processor is kept awake past the workout's end, in case the last tick runs late. */
private const val AWAKE_MARGIN_MILLIS = 5_000L

/** What the watch's interval timer is doing. */
sealed interface IntervalState {
    /** No cardio or mobility workout is running. */
    data object Idle : IntervalState

    /**
     * The clock is going, or paused.
     *
     * @property workout The workout being done.
     * @property run Where it is.
     */
    data class Running(val workout: IntervalWorkout, val run: IntervalRun) : IntervalState

    /**
     * Finished, or ended early.
     *
     * @property workout The workout that was done.
     * @property workedSeconds Seconds of work done, rest and switches left out.
     * @property save Whether it has been queued for the phone yet.
     */
    data class Done(val workout: IntervalWorkout, val workedSeconds: Int, val save: IntervalSave) : IntervalState
}

/** Where keeping a finished interval workout is up to. */
enum class IntervalSave {
    /** No work was done, so there is nothing to keep. */
    NOTHING,

    /** Being written to the encrypted database. */
    SAVING,

    /** Queued for the phone, which adds it to the history at the next sync. */
    SAVED,

    /** The data is locked on this device, so it couldn't be written. */
    FAILED,
}

/**
 * Runs a cardio or mobility workout on the watch: counts each step down
 * against the clock that ignores time-zone and clock changes, buzzes as
 * steps change, and queues the work done for the phone when it ends.
 *
 * It lives as long as the app, not a screen, so the timer keeps going when
 * the person lowers their wrist or swipes back to the home screen. The
 * workout service keeps the app alive meanwhile when it may run. The run is
 * held in memory only: if the system kills the app mid-workout it is lost,
 * as on the phone, so nothing about it sits on disk unfinished.
 *
 * Nothing here touches the coach. These workouts never change the plan or
 * count toward the week's hard sets.
 */
@Singleton
class IntervalController @Inject constructor(
    private val outbox: IntervalOutbox,
    private val clock: TrainingClock,
    private val signals: IntervalSignals,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<IntervalState>(IntervalState.Idle)

    // Ticks and taps change the run from different threads; one at a time keeps each cue to once.
    private val mutex = Mutex()
    private var clockJob: Job? = null
    private var workoutId = ""
    private var startedAtEpochMillis = 0L
    private var anchorElapsedMillis = 0L

    /** What the timer is doing now. */
    val state: StateFlow<IntervalState> = mutableState.asStateFlow()

    /**
     * Starts the workout with [workoutId]. Ignored while another one is
     * running, so a second tap can't throw away the first.
     */
    fun start(workoutId: String) = act {
        val workout = IntervalCatalogue.byId(workoutId)
        if (workout != null && mutableState.value !is IntervalState.Running) {
            // A random id, like strength workouts, so the history groups the moves as one workout.
            this.workoutId = UUID.randomUUID().toString()
            startedAtEpochMillis = clock.currentTimeMillis()
            anchorElapsedMillis = clock.elapsedRealtimeMillis()
            val run = IntervalRun(IntervalTimeline.of(workout))
            mutableState.value = IntervalState.Running(workout, run)
            signals.stayAwakeFor(run.awakeMillis())
            clockJob = scope.launch { runClock() }
        }
    }

    /** Stops or restarts the clock. */
    fun togglePause() = act {
        val running = mutableState.value as? IntervalState.Running ?: return@act
        val run = running.run.togglePause()
        // Time spent paused never counts: the count starts again from now.
        anchorElapsedMillis = clock.elapsedRealtimeMillis()
        mutableState.value = running.copy(run = run)
        if (run.paused) signals.letSleep() else signals.stayAwakeFor(run.awakeMillis())
    }

    /** Ends the step under way now. */
    fun skip() = act { move { it.skip() } }

    /** Ends the workout now, keeping what was done. */
    fun end() = act {
        (mutableState.value as? IntervalState.Running)?.let { finish(it) }
    }

    /** Clears the done screen. */
    fun dismiss() = act {
        if (mutableState.value is IntervalState.Done) mutableState.value = IntervalState.Idle
    }

    /**
     * Moves the run on by the whole seconds that have passed on the elapsed
     * clock since the last tick. The clock loop calls it a few times a
     * second; tests call it directly.
     */
    internal fun tick() = act {
        val running = mutableState.value as? IntervalState.Running ?: return@act
        val now = clock.elapsedRealtimeMillis()
        if (running.run.paused) {
            anchorElapsedMillis = now
        } else {
            val seconds = ((now - anchorElapsedMillis) / MILLIS_PER_SECOND).toInt()
            anchorElapsedMillis += seconds * MILLIS_PER_SECOND
            if (seconds > 0) move { it.tick(seconds) }
        }
    }

    private suspend fun runClock() {
        while (mutableState.value is IntervalState.Running) {
            delay(INTERVAL_TICK_MILLIS)
            tick()
        }
    }

    /** Applies [change], buzzes its cue, and finishes when the run is done. Call inside [act]. */
    private fun move(change: (IntervalRun) -> IntervalRun) {
        val running = mutableState.value as? IntervalState.Running ?: return
        val run = change(running.run)
        run.cueFrom(running.run)?.let(signals::play)
        val moved = running.copy(run = run)
        if (run.isFinished) finish(moved) else mutableState.value = moved
    }

    /** Shows the done screen and queues the work, if any, for the phone. Call inside [act]. */
    private fun finish(running: IntervalState.Running) {
        clockJob?.cancel()
        clockJob = null
        signals.letSleep()
        val done = running.run.sessions(startedAtEpochMillis, clock.currentTimeMillis())
        mutableState.value = IntervalState.Done(
            workout = running.workout,
            workedSeconds = done.sumOf { session -> session.sets.sumOf { it.value } },
            save = if (done.isEmpty()) IntervalSave.NOTHING else IntervalSave.SAVING,
        )
        if (done.isNotEmpty()) {
            val id = workoutId
            scope.launch {
                val saved = try {
                    outbox.queue(QueuedIntervalWorkout(id, done))
                    IntervalSave.SAVED
                } catch (error: DataUnavailableException) {
                    IntervalSave.FAILED
                }
                val current = mutableState.value
                if (current is IntervalState.Done) mutableState.value = current.copy(save = saved)
            }
        }
    }

    /** Runs [action] after any action already running. */
    private fun act(action: suspend () -> Unit) {
        scope.launch { mutex.withLock { action() } }
    }
}

/** How long to keep the processor on so the last buzz comes on time with the screen off. */
private fun IntervalRun.awakeMillis(): Long = totalRemaining * MILLIS_PER_SECOND + AWAKE_MARGIN_MILLIS

/**
 * What the interval timer does to the watch itself: buzzes and the wake
 * lock that keeps them on time with the screen off. An interface so the
 * timer can be tested without a watch.
 */
interface IntervalSignals {
    /** Buzzes for [cue]. */
    fun play(cue: IntervalCue)

    /** Keeps the processor on for [millis]. A later call replaces the time limit. */
    fun stayAwakeFor(millis: Long)

    /** Lets the processor sleep again. */
    fun letSleep()
}
