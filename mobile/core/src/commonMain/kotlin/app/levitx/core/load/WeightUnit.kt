package app.levitx.core.load

import kotlin.math.roundToLong

/** Kilograms in one pound, exactly, by international agreement. */
public const val KG_PER_POUND: Double = 0.45359237

private const val HUNDREDTHS_PER_UNIT = 100L
private const val HUNDREDTHS_PER_TENTH = 10L

/**
 * The unit weights are shown and entered in. Everything is stored in
 * kilograms; this only changes what the person sees and types.
 */
public enum class WeightUnit(private val kgPerUnit: Double) {
    /** Kilograms. */
    KG(1.0),

    /** Pounds. */
    LB(KG_PER_POUND),
    ;

    /** [kg] expressed in this unit. */
    public fun fromKg(kg: Double): Double = kg / kgPerUnit

    /** [value] in this unit, as kilograms. */
    public fun toKg(value: Double): Double = value * kgPerUnit
}

/**
 * [kg] in [unit], as people write it: up to two decimal places, without
 * trailing zeros. "1.25", "2.5", "45".
 */
public fun formatWeight(kg: Double, unit: WeightUnit): String {
    val hundredths = (unit.fromKg(kg) * HUNDREDTHS_PER_UNIT).roundToLong()
    val whole = hundredths / HUNDREDTHS_PER_UNIT
    val part = hundredths % HUNDREDTHS_PER_UNIT
    return when {
        part == 0L -> whole.toString()
        part % HUNDREDTHS_PER_TENTH == 0L -> "$whole.${part / HUNDREDTHS_PER_TENTH}"
        else -> "$whole.${part.toString().padStart(2, '0')}"
    }
}
