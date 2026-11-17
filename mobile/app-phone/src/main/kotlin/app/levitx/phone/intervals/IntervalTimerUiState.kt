package app.levitx.phone.intervals

import app.levitx.core.intervals.IntervalPhase
import app.levitx.core.intervals.IntervalRun
import app.levitx.core.intervals.IntervalWorkout
import app.levitx.phone.workout.Cue

/** What the interval timer shows. */
sealed interface IntervalTimerUiState {
    /** The workout id isn't in the catalogue, say after an update removed it. */
    data object NotFound : IntervalTimerUiState

    /**
     * The clock is going, or paused.
     *
     * @property workout The workout being done.
     * @property run Where it is.
     * @property askingToEnd True while the "End now?" sheet is up. The clock keeps going behind it.
     */
    data class Running(val workout: IntervalWorkout, val run: IntervalRun, val askingToEnd: Boolean = false) :
        IntervalTimerUiState

    /**
     * Finished, or ended early.
     *
     * @property workout The workout that was done.
     * @property workedSeconds Seconds of work done, rest and switches left out.
     * @property save Whether it has gone into the history yet.
     */
    data class Done(val workout: IntervalWorkout, val workedSeconds: Int, val save: SaveStatus) : IntervalTimerUiState
}

/** Where saving a finished interval workout is up to. */
enum class SaveStatus {
    /** No work was done, so there is nothing to keep. */
    NOTHING,

    /** Being written to the encrypted database. */
    SAVING,

    /** In the history. */
    SAVED,

    /** The data is locked on this device, so it couldn't be written. */
    FAILED,
}

/** The last seconds of each step that get a countdown beep. */
private const val COUNTDOWN_SECONDS = 3

/**
 * The cue to play going from [before] to [after], or null for none.
 *
 * A finished workout gets the done chime; a new work step gets GO; rest and
 * switching sides get the double beep; the last three seconds of any step
 * count down. When a late tick jumps several seconds only the newest cue
 * plays, so nothing ever plays twice or out of order.
 */
internal fun intervalCue(before: IntervalRun, after: IntervalRun): Cue? = when {
    after.isFinished -> Cue.DONE.takeIf { !before.isFinished }
    after.index != before.index -> if (after.current?.phase == IntervalPhase.WORK) Cue.GO else Cue.TARGET
    after.remaining in 1..COUNTDOWN_SECONDS && after.remaining != before.remaining -> Cue.COUNT
    else -> null
}
