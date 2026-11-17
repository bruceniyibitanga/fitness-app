package app.levitx.core.intervals

import app.levitx.core.session.CompletedSession
import app.levitx.core.session.LoggedSet

/**
 * Where an interval workout is: which step, how long is left of it, and how
 * much of each work step was done. Immutable; every change returns a new run,
 * so the screen and a saved draft can never disagree.
 *
 * @property steps Every step, from [IntervalTimeline.of].
 * @property index The step under way, or [steps].size once finished.
 * @property remaining Seconds left of the step under way.
 * @property paused True while the clock is stopped.
 * @property worked Seconds done of each step so far, by step index. Only work steps count.
 * @throws IllegalArgumentException if [steps] is empty or [index] or [remaining] is out of range.
 */
public data class IntervalRun(
    val steps: List<IntervalStep>,
    val index: Int = 0,
    val remaining: Int = steps.firstOrNull()?.seconds ?: 0,
    val paused: Boolean = false,
    val worked: Map<Int, Int> = emptyMap(),
) {
    init {
        require(steps.isNotEmpty()) { "a run needs at least one step" }
        require(index in 0..steps.size) { "index out of range" }
        require(remaining >= 0) { "remaining must not be negative" }
    }

    /** True once every step is done or skipped. */
    val isFinished: Boolean
        get() = index >= steps.size

    /** The step under way, or null once finished. */
    val current: IntervalStep?
        get() = steps.getOrNull(index)

    /** The next work step after this one, for "next up", or null when this is the last. */
    val nextWork: IntervalStep?
        get() = steps.drop(index + 1).firstOrNull { it.phase == IntervalPhase.WORK }

    /** Seconds left in the whole workout, counting the rest of this step. */
    val totalRemaining: Int
        get() = remaining + steps.drop(index + 1).sumOf { it.seconds }

    /**
     * The run [seconds] later. Steps that run out hand their leftover time to
     * the next, so a late tick never loses time. Paused runs don't move.
     */
    public fun tick(seconds: Int): IntervalRun {
        require(seconds >= 0) { "seconds must not be negative" }
        var run = this
        var left = seconds
        while (!run.paused && !run.isFinished && left > 0) {
            val used = minOf(left, run.remaining)
            run = run.counting(used).copy(remaining = run.remaining - used)
            left -= used
            if (run.remaining == 0) run = run.advanced()
        }
        return run
    }

    /** Ends the step under way now, keeping whatever of it was done. */
    public fun skip(): IntervalRun = if (isFinished) this else advanced()

    /** Stops or restarts the clock. */
    public fun togglePause(): IntervalRun = copy(paused = !paused)

    /**
     * The work done, as one session per move, in the order they were done.
     * Each round becomes a set of the seconds actually worked. Moves skipped
     * entirely are left out, so a workout ended early logs only what happened.
     * Empty when no work was done at all.
     */
    public fun sessions(startedAtEpochMillis: Long, finishedAtEpochMillis: Long): List<CompletedSession> =
        worked.entries
            .filter { (step, seconds) -> steps[step].phase == IntervalPhase.WORK && seconds > 0 }
            .sortedBy { it.key }
            .groupBy { steps[it.key].move.id }
            .map { (moveId, done) ->
                CompletedSession(
                    gateId = moveId,
                    startedAtEpochMillis = startedAtEpochMillis,
                    finishedAtEpochMillis = finishedAtEpochMillis,
                    sets = done.map { LoggedSet(value = it.value, repsInReserve = null, isBestEffort = false) },
                )
            }

    /** This run with [seconds] more done of the step under way, when it's a work step. */
    private fun counting(seconds: Int): IntervalRun = if (current?.phase == IntervalPhase.WORK && seconds > 0) {
        copy(worked = worked + (index to (worked[index] ?: 0) + seconds))
    } else {
        this
    }

    private fun advanced(): IntervalRun {
        val next = index + 1
        return copy(index = next, remaining = steps.getOrNull(next)?.seconds ?: 0)
    }
}
