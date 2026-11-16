package app.levitx.core.load

import kotlin.math.roundToLong

/** How weight is carried on an exercise. */
public enum class LoadCarrier {
    /** A weighted vest, worn for anything done standing, hanging or on the floor. */
    VEST,

    /** A dip belt with plates hanging from it, for pull-ups and dips. */
    BELT,

    /** Plates resting on the hips, for bridges and hip thrusts. */
    PLATES,
}

/** Most plates the kit keeps track of. More than anyone hangs from a belt. */
private const val MAX_PLATES = 24

/** Heaviest single plate accepted, in kilograms. Anything heavier is almost certainly a typo. */
private const val MAX_PLATE_KG = 50.0

/** Weights are compared and kept to the gram, so 2.5 + 2.5 is exactly 5. */
private const val GRAMS_PER_KG = 1000.0

/**
 * The weight a person can add to their training.
 *
 * Calisthenics comes first in Levitx, so this is the kit people add to
 * bodyweight work: a vest, and plates for a dip belt or the hips. Dumbbells
 * and kettlebells are for later, when the app covers weights as well.
 *
 * Everything is in kilograms. The apps convert for people who think in pounds,
 * so a 45 lb plate is stored as its exact weight in kilograms.
 *
 * @property vestMaxKg The heaviest the vest goes. 0 for no vest.
 * @property vestStepKg The steps it adjusts in, such as 1 kg. 0 for a vest of
 *   one fixed weight, [vestMaxKg].
 * @property platesKg Each plate or loose weight they own, one entry per plate.
 * @property hasBelt True when they own a dip belt to hang the plates from.
 * @throws IllegalArgumentException if a weight is out of range or there are too many plates.
 */
public data class LoadKit(
    val vestMaxKg: Double = 0.0,
    val vestStepKg: Double = 0.0,
    val platesKg: List<Double> = emptyList(),
    val hasBelt: Boolean = false,
) {
    init {
        require(vestMaxKg in 0.0..LoadLimits.MAX_MODIFIER_KG) { "vestMaxKg out of range" }
        require(vestStepKg in 0.0..vestMaxKg) { "vestStepKg must be between 0 and the vest's weight" }
        require(platesKg.size <= MAX_PLATES) { "too many plates" }
        require(platesKg.all { it > 0.0 && it <= MAX_PLATE_KG }) { "a plate's weight is out of range" }
    }

    /** True when there is nothing to add weight with. */
    val isEmpty: Boolean
        get() = vestMaxKg == 0.0 && platesKg.isEmpty()

    /**
     * Every added weight possible with the [carriers] an exercise allows,
     * lightest first, starting with 0. A vest and a belt can be worn together.
     */
    public fun choices(carriers: Set<LoadCarrier>): List<Double> {
        val vest = if (LoadCarrier.VEST in carriers) vestLevels() else listOf(0L)
        val usesPlates = (LoadCarrier.BELT in carriers && hasBelt) || LoadCarrier.PLATES in carriers
        val plates = if (usesPlates) plateSums() else setOf(0L)
        val limit = (LoadLimits.MAX_MODIFIER_KG * GRAMS_PER_KG).roundToLong()
        return vest.flatMap { v -> plates.map { p -> v + p } }
            .filter { it <= limit }
            .distinct()
            .sorted()
            .map { it / GRAMS_PER_KG }
    }

    /** The vest's settings in grams: none, then each step up to its full weight. */
    private fun vestLevels(): List<Long> {
        val max = vestMaxKg.grams()
        val step = vestStepKg.grams()
        if (max == 0L) return listOf(0L)
        val steps = if (step == 0L) emptyList() else (step until max step step).toList()
        return listOf(0L) + steps + max
    }

    /** Every total the plates can make, in grams, 0 included. */
    private fun plateSums(): Set<Long> = platesKg.fold(setOf(0L)) { sums, plate ->
        sums + sums.map { it + plate.grams() }
    }
}

private fun Double.grams(): Long = (this * GRAMS_PER_KG).roundToLong()
