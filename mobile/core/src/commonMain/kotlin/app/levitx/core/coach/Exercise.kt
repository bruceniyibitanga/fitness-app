package app.levitx.core.coach

import app.levitx.core.load.LoadProfile
import app.levitx.core.skills.GateMeasure

/**
 * One exercise the coach can put in a session.
 *
 * @property id Stable identifier used in storage. Never reuse or rename one.
 * @property name Name shown in the apps.
 * @property pattern The movement it trains directly.
 * @property measure Whether sets count reps or seconds held.
 * @property range The reps or seconds per set this exercise is trained in. Someone
 *   who reaches the top of it moves up a rung.
 * @property equipment Kit it needs. Empty for floor exercises.
 * @property alsoTrains Patterns it trains indirectly. A set counts half toward each.
 * @property loadProfile Share of bodyweight moved per rep, where we have a measured
 *   or reasoned figure. Null means the apps show no kilograms for it.
 * @property isPerSide True when each set is done once on each side.
 * @throws IllegalArgumentException if [range] is empty or starts below 1, or
 *   [alsoTrains] repeats [pattern].
 */
public data class Exercise(
    val id: String,
    val name: String,
    val pattern: MovementPattern,
    val measure: GateMeasure,
    val range: IntRange,
    val equipment: Set<Equipment> = emptySet(),
    val alsoTrains: Set<MovementPattern> = emptySet(),
    val loadProfile: LoadProfile? = null,
    val isPerSide: Boolean = false,
) {
    init {
        require(range.first >= 1 && range.last >= range.first) { "range must start at 1 or more and not be empty" }
        require(pattern !in alsoTrains) { "alsoTrains must not repeat the main pattern" }
    }

    /** How much a target moves at a time: one rep, or [CoachTuning.HOLD_STEP_SECONDS] for holds. */
    val step: Int
        get() = if (measure == GateMeasure.HOLD_SECONDS) CoachTuning.HOLD_STEP_SECONDS else 1

    /** True when the person has everything this exercise needs. */
    public fun fits(owned: Set<Equipment>): Boolean = owned.containsAll(equipment)

    /**
     * How [sets] of this exercise count toward each pattern: in full toward
     * [pattern], and [CoachTuning.INDIRECT_SET_SHARE] toward each of [alsoTrains].
     */
    public fun setsByPattern(sets: Int): Map<MovementPattern, Double> = buildMap {
        put(pattern, sets.toDouble())
        alsoTrains.forEach { put(it, sets * CoachTuning.INDIRECT_SET_SHARE) }
    }
}

/**
 * One step of difficulty on a [Ladder].
 *
 * @property options Exercises of about the same difficulty that train the same
 *   thing. The first is the usual pick; the others are what the coach rotates
 *   to between blocks, or falls back on when kit is missing.
 * @throws IllegalArgumentException if [options] is empty.
 */
public data class Rung(val options: List<Exercise>) {
    init {
        require(options.isNotEmpty()) { "a rung needs at least one exercise" }
    }
}

/**
 * The exercises for one movement pattern, from easiest to hardest.
 *
 * @property pattern The pattern every exercise on the ladder trains.
 * @property rungs Steps of difficulty, easiest first.
 * @throws IllegalArgumentException if [rungs] is empty or holds an exercise of another pattern.
 */
public data class Ladder(val pattern: MovementPattern, val rungs: List<Rung>) {
    init {
        require(rungs.isNotEmpty()) { "a ladder needs at least one rung" }
        require(rungs.all { rung -> rung.options.all { it.pattern == pattern } }) {
            "every exercise on a ladder must train its pattern"
        }
    }

    /**
     * The highest rung at or below [rung] that has an exercise the person can
     * do with the kit they [owned], or null when there is none.
     */
    public fun usableRungAtOrBelow(rung: Int, owned: Set<Equipment>): Int? =
        (rung.coerceIn(0, rungs.lastIndex) downTo 0).firstOrNull { usableAt(it, owned).isNotEmpty() }

    /** The next rung above [rung] the person has the kit for, or null at the top. */
    public fun usableRungAbove(rung: Int, owned: Set<Equipment>): Int? =
        (rung + 1..rungs.lastIndex).firstOrNull { usableAt(it, owned).isNotEmpty() }

    /**
     * The exercise to train at [rung]. When the rung has several the person can
     * do, [variant] picks between them, wrapping round, so a different number
     * each block rotates through them in a fixed order.
     *
     * @return null when [rung] has nothing the person has the kit for.
     */
    public fun exerciseAt(rung: Int, owned: Set<Equipment>, variant: Int = 0): Exercise? {
        val usable = rungs.getOrNull(rung)?.let { usableAt(rung, owned) }.orEmpty()
        return usable.getOrNull(variant.mod(usable.size.coerceAtLeast(1)))
    }

    private fun usableAt(rung: Int, owned: Set<Equipment>): List<Exercise> = rungs[rung].options.filter {
        it.fits(owned)
    }
}
