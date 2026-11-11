package app.levitx.core.load

/**
 * Works out how much help a band gives from a bathroom-scale reading.
 *
 * The person stands on a scale under the bar with one foot in the band, at
 * the depth their chin would be halfway up a pull-up, hands off the bar. The
 * band pulls up on them, so the scale reads less than their bodyweight; the
 * difference is the band's help at that stretch.
 *
 * A band helps most at the bottom of a rep and least at the top, so measuring
 * halfway gives a fair average. It is still an estimate, which is why loads
 * with a band are always shown as estimates.
 *
 * @return Kilograms of help, or null if the reading isn't less than
 *   [bodyweightKg] or the result is outside what a band can plausibly give.
 */
public fun bandAssistanceFromScale(bodyweightKg: Double, scaleReadingKg: Double): Double? {
    if (!bodyweightKg.isFinite() || !scaleReadingKg.isFinite() || scaleReadingKg < 0.0) return null
    val assistance = bodyweightKg - scaleReadingKg
    return assistance.takeIf { it > 0.0 && it <= LoadLimits.MAX_MODIFIER_KG }
}
