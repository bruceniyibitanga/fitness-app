package app.levitx.core.intervals

import app.levitx.core.coach.Exercise

/** What the person is doing during a step. */
public enum class IntervalPhase {
    /** Getting ready before the first move. */
    PREPARE,

    /** The move itself. */
    WORK,

    /** Swapping to the other side of a one-sided move. */
    SWITCH,

    /** Resting before the next round or move. */
    REST,
}

/** Which side a one-sided move is on. */
public enum class Side {
    /** Left side first. */
    LEFT,

    /** Then the right. */
    RIGHT,
}

/**
 * One timed step of an interval workout.
 *
 * @property phase What happens during it.
 * @property move The move of the block it belongs to. Getting ready
 *   belongs to the first move.
 * @property seconds How long it lasts.
 * @property blockIndex Which block of the workout it's in.
 * @property round Which round of the block, from 1.
 * @property rounds How many rounds the block has.
 * @property side The side for a one-sided move, or null.
 */
public data class IntervalStep(
    val phase: IntervalPhase,
    val move: Exercise,
    val seconds: Int,
    val blockIndex: Int,
    val round: Int,
    val rounds: Int,
    val side: Side? = null,
)

/** Turns an interval workout into the timed steps the timer counts down. */
public object IntervalTimeline {
    /** Seconds to get into position before the first move. */
    public const val PREPARE_SECONDS: Int = 10

    /** Seconds to swap sides on a one-sided move. */
    public const val SWITCH_SECONDS: Int = 5

    /**
     * Every step of [workout] in order: getting ready, then each block's
     * rounds, both sides of a one-sided move, with rest after each round.
     * There's no rest after the very last round: the workout is done.
     */
    public fun of(workout: IntervalWorkout): List<IntervalStep> {
        val first = workout.blocks.first()
        val prepare = IntervalStep(IntervalPhase.PREPARE, first.move, PREPARE_SECONDS, 0, 1, first.rounds)
        val body = workout.blocks.flatMapIndexed { index, block -> blockSteps(index, block) }
        return listOf(prepare) + body.dropLastWhile { it.phase == IntervalPhase.REST }
    }

    private fun blockSteps(index: Int, block: IntervalBlock): List<IntervalStep> = (1..block.rounds).flatMap { round ->
        fun step(phase: IntervalPhase, seconds: Int, side: Side? = null) =
            IntervalStep(phase, block.move, seconds, index, round, block.rounds, side)
        val work = if (block.move.isPerSide) {
            listOf(
                step(IntervalPhase.WORK, block.workSeconds, Side.LEFT),
                step(IntervalPhase.SWITCH, SWITCH_SECONDS, Side.RIGHT),
                step(IntervalPhase.WORK, block.workSeconds, Side.RIGHT),
            )
        } else {
            listOf(step(IntervalPhase.WORK, block.workSeconds))
        }
        val rest = if (round == block.rounds) block.restAfterSeconds else block.restSeconds
        work + listOfNotNull(step(IntervalPhase.REST, rest).takeIf { rest > 0 })
    }
}
