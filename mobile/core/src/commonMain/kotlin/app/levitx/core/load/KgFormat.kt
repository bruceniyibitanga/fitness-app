package app.levitx.core.load

import kotlin.math.roundToLong

private const val TENTHS_PER_UNIT = 10L

/**
 * Kilograms as people write them: "72.5" or "70". One decimal place at most,
 * and none when it's a whole number.
 */
public fun formatKg(kg: Double): String {
    val tenths = (kg * TENTHS_PER_UNIT).roundToLong()
    return if (tenths % TENTHS_PER_UNIT == 0L) {
        (tenths / TENTHS_PER_UNIT).toString()
    } else {
        "${tenths / TENTHS_PER_UNIT}.${tenths % TENTHS_PER_UNIT}"
    }
}
