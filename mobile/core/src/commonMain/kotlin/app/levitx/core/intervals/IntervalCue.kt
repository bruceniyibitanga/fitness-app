package app.levitx.core.intervals

/**
 * What an interval timer signals as the run moves on. Each device turns
 * these into its own beeps or buzzes; the rules for when they happen live
 * here so the phone and the watch always agree.
 */
public enum class IntervalCue {
    /** A work step starts. */
    GO,

    /** Rest, getting ready or switching sides starts. */
    CHANGE,

    /** One of the last three seconds of a step. */
    COUNT,

    /** The workout is over. */
    DONE,
}

/** The last seconds of each step that get a countdown cue. */
private const val COUNTDOWN_SECONDS = 3

/**
 * The cue for going from [before] to this run, or null for none.
 *
 * Finishing gets [IntervalCue.DONE] once; a new work step gets
 * [IntervalCue.GO]; any other new step gets [IntervalCue.CHANGE]; the last
 * three seconds of any step get [IntervalCue.COUNT]. When a late tick jumps
 * several seconds only the newest cue plays, so nothing plays twice or out
 * of order.
 */
public fun IntervalRun.cueFrom(before: IntervalRun): IntervalCue? = when {
    isFinished -> IntervalCue.DONE.takeIf { !before.isFinished }
    index != before.index -> if (current?.phase == IntervalPhase.WORK) IntervalCue.GO else IntervalCue.CHANGE
    remaining in 1..COUNTDOWN_SECONDS && remaining != before.remaining -> IntervalCue.COUNT
    else -> null
}
