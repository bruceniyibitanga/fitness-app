package app.levitx.core.intervals

import app.levitx.core.coach.Equipment
import app.levitx.core.coach.Exercise
import app.levitx.core.coach.MovementPattern

/** Whether a timed workout is for getting out of breath or for moving through range. */
public enum class IntervalKind {
    /** Work and rest intervals that raise the heart rate. */
    CARDIO,

    /** Stretches and range work, held for time. */
    MOBILITY,
}

/**
 * One move done for [rounds] rounds of [workSeconds], with [restSeconds]
 * after each round. A move done on each side is done on both sides every
 * round, with a short switch between them.
 *
 * @property move The move. Its pattern is cardio or mobility.
 * @property workSeconds How long each round of work lasts.
 * @property restSeconds Rest after each round.
 * @property rounds How many rounds.
 * @property restAfterSeconds Rest after the last round, before the next block.
 * @throws IllegalArgumentException if a number is out of range or [move] isn't a cardio or mobility move.
 */
public data class IntervalBlock(
    val move: Exercise,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
    val restAfterSeconds: Int = restSeconds,
) {
    init {
        require(!move.pattern.isStrength) { "${move.id} is a strength exercise, not an interval move" }
        require(workSeconds in 1..MAX_STEP_SECONDS) { "workSeconds must be 1 to $MAX_STEP_SECONDS" }
        require(restSeconds in 0..MAX_STEP_SECONDS) { "restSeconds must be 0 to $MAX_STEP_SECONDS" }
        require(rounds in 1..MAX_ROUNDS) { "rounds must be 1 to $MAX_ROUNDS" }
        require(restAfterSeconds in 0..MAX_STEP_SECONDS) { "restAfterSeconds must be 0 to $MAX_STEP_SECONDS" }
    }
}

/**
 * A timed workout: cardio intervals or a mobility routine, run on the
 * interval timer. These are separate from the coach's plan on purpose. They
 * are logged in history but never count toward strength volume or progress.
 *
 * @property id Stable identifier used in storage.
 * @property name What it's called.
 * @property kind Cardio or mobility.
 * @property about One or two sentences on what it is and where it comes from.
 * @property blocks The moves, in order.
 * @throws IllegalArgumentException if [blocks] is empty.
 */
public data class IntervalWorkout(
    val id: String,
    val name: String,
    val kind: IntervalKind,
    val about: String,
    val blocks: List<IntervalBlock>,
) {
    init {
        require(blocks.isNotEmpty()) { "an interval workout needs at least one block" }
    }

    /** Every piece of kit some block needs. */
    val equipment: Set<Equipment>
        get() = blocks.flatMap { it.move.equipment }.toSet()

    /** How long it takes start to finish, rounded up to whole minutes. */
    val minutes: Int
        get() = (IntervalTimeline.of(this).sumOf { it.seconds } + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE
}

/** The patterns interval moves belong to. */
internal val intervalPatterns: Set<MovementPattern> = setOf(MovementPattern.CARDIO, MovementPattern.MOBILITY)

/** The longest a single work, rest or switch step can be. */
internal const val MAX_STEP_SECONDS = 600

/** The most rounds a block can have. */
internal const val MAX_ROUNDS = 20

private const val SECONDS_PER_MINUTE = 60
