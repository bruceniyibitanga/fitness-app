package app.levitx.phone.live

import app.levitx.core.intervals.IntervalPhase
import app.levitx.phone.workout.SetTimer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The workout in progress, as the cues and the workout notification need it.
 *
 * It holds no reps, loads or anything else personal beyond what the
 * notification shows: the exercise, where the person is up to, and the clocks.
 *
 * @property exerciseName The exercise up next (or after this rest).
 * @property exerciseNumber Which exercise that is, from 1.
 * @property exerciseCount Exercises in the workout.
 * @property setNumber Which set of the exercise is up next, from 1.
 * @property setCount Sets planned for the exercise.
 * @property setsPlanned Sets planned for each exercise, in order, for the progress bar.
 * @property setsBehind Sets the person has moved past: done, or skipped with their exercise.
 * @property restEndsAtElapsedMillis When rest runs out on the elapsed clock, or null when not resting.
 * @property timer The hold or negative timer while it runs.
 * @property target Seconds to hold, or negatives to pace, for the set up next.
 * @property passedGate The gate just passed, so its cue plays once, or null.
 * @property sounds True when cues beep.
 * @property buzzes True when cues buzz.
 */
data class LiveSession(
    val exerciseName: String,
    val exerciseNumber: Int,
    val exerciseCount: Int,
    val setNumber: Int,
    val setCount: Int,
    val setsPlanned: List<Int>,
    val setsBehind: Int,
    val restEndsAtElapsedMillis: Long? = null,
    val timer: SetTimer? = null,
    val target: Int = 0,
    val passedGate: String? = null,
    val sounds: Boolean = true,
    val buzzes: Boolean = true,
)

/**
 * A cardio or mobility workout in progress, as its notification needs it:
 * which workout, and the step it is on. Nothing from the person's log.
 *
 * @property workoutName The workout, for the notification's title.
 * @property phase What the step under way is: work, rest, getting ready or switching sides.
 * @property moveName The move of that step.
 * @property round Which round of the move, from 1.
 * @property rounds How many rounds the move has.
 * @property paused True while the person has paused the clock.
 */
data class LiveIntervals(
    val workoutName: String,
    val phase: IntervalPhase,
    val moveName: String,
    val round: Int,
    val rounds: Int,
    val paused: Boolean,
)

/**
 * Where the workout screen tells the rest of the app what is happening.
 *
 * The workout itself stays with the screen's view model, which saves it after
 * every change. This only carries a read-only picture of it to the two things
 * that have to keep going when the screen is off or another app is in front:
 * [WorkoutCuer], which beeps and buzzes, and [LiveWorkoutService], which shows
 * the workout on the lock screen and keeps the app running. The interval
 * timer publishes its own, smaller picture ([LiveIntervals]) for the service.
 */
@Singleton
class LiveWorkout @Inject constructor() {
    private val current = MutableStateFlow<LiveSession?>(null)
    private val dismissed = MutableStateFlow(false)
    private var owner: Any? = null
    private val timed = MutableStateFlow<LiveIntervals?>(null)
    private var timedOwner: Any? = null

    /** The workout in progress, or null when there is none. */
    val session: StateFlow<LiveSession?> = current.asStateFlow()

    /**
     * The cardio or mobility workout in progress, or null when there is
     * none. [LiveWorkoutService] follows it the same way as [session], which
     * is what keeps the interval timer beeping with another app in front.
     */
    val intervals: StateFlow<LiveIntervals?> = timed.asStateFlow()

    /** True once the person swipes the workout notification away. It isn't shown again until the next workout. */
    val noticeDismissed: StateFlow<Boolean> = dismissed.asStateFlow()

    /**
     * Shows [session] as the workout in progress. [from] is whoever publishes
     * it, so that a screen being closed can't clear a workout a newer screen
     * has already started.
     */
    fun publish(from: Any, session: LiveSession?) {
        if (owner !== from) dismissed.value = false
        owner = from
        current.value = session
    }

    /** Clears the workout, if [from] is still the one showing it. */
    fun clear(from: Any) {
        if (owner !== from) return
        owner = null
        current.value = null
        dismissed.value = false
    }

    /** Shows [intervals] as the cardio or mobility workout in progress. [from] is the timer publishing it. */
    fun publishIntervals(from: Any, intervals: LiveIntervals) {
        if (timedOwner !== from) dismissed.value = false
        timedOwner = from
        timed.value = intervals
    }

    /** Clears the cardio or mobility workout, if [from] is still the timer showing it. */
    fun clearIntervals(from: Any) {
        if (timedOwner !== from) return
        timedOwner = null
        timed.value = null
        dismissed.value = false
    }

    /** Records that the person swiped the notification away. */
    fun dismissNotice() {
        dismissed.value = true
    }
}
