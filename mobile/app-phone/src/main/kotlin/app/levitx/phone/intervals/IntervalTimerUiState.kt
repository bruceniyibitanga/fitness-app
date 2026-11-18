package app.levitx.phone.intervals

import app.levitx.core.intervals.IntervalCue
import app.levitx.core.intervals.IntervalRun
import app.levitx.core.intervals.IntervalWorkout
import app.levitx.core.intervals.cueFrom
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

/**
 * The sound and buzz for going from [before] to [after], or null for none.
 * When each cue happens is decided in core (`cueFrom`), so the watch does
 * the same; this only picks the phone's sound for it: GO for work, the
 * double beep for rest and switching sides, the count beep, the done chime.
 */
internal fun intervalCue(before: IntervalRun, after: IntervalRun): Cue? = when (after.cueFrom(before)) {
    IntervalCue.GO -> Cue.GO
    IntervalCue.CHANGE -> Cue.TARGET
    IntervalCue.COUNT -> Cue.COUNT
    IntervalCue.DONE -> Cue.DONE
    null -> null
}
