package app.levitx.core.load

import app.levitx.core.requireInRange

/**
 * Input limits for [calculateEffectiveLoad].
 *
 * Values outside these are almost certainly typos (a bodyweight of 7 kg, say),
 * so the calculation refuses them instead of producing nonsense.
 */
public object LoadLimits {
    /** Lowest bodyweight accepted, in kilograms. */
    public const val MIN_BODYWEIGHT_KG: Double = 20.0

    /** Highest bodyweight accepted, in kilograms. */
    public const val MAX_BODYWEIGHT_KG: Double = 350.0

    /** Most weight a single modifier may add or remove, in kilograms. */
    public const val MAX_MODIFIER_KG: Double = 200.0
}

/**
 * The load of one rep, with the parts that make it up.
 *
 * Keeping the parts lets the apps explain a number ("47.4 kg: 64% of your
 * 74 kg") instead of showing a figure nobody can check.
 *
 * @property fromBodyweightKg Kilograms of bodyweight moved.
 * @property fromAddedWeightKg Kilograms moved from vests, belts and other added weight.
 * @property removedByAssistanceKg Kilograms taken off by band assistance.
 * @property isEstimate True when the exercise's share is an estimate.
 */
public data class EffectiveLoad(
    val fromBodyweightKg: Double,
    val fromAddedWeightKg: Double,
    val removedByAssistanceKg: Double,
    val isEstimate: Boolean,
) {
    /** Total load per rep in kilograms. Never below zero. */
    val totalKg: Double
        get() = (fromBodyweightKg + fromAddedWeightKg - removedByAssistanceKg).coerceAtLeast(0.0)
}

/**
 * Works out how many kilograms one rep of an exercise moves.
 *
 * Added weight is counted at the same [LoadProfile.bodyweightShare] as the
 * body. That is a fair estimate for a vest worn on the torso, and exact for
 * hanging exercises like pull-ups where the share is 1. Band assistance is
 * subtracted in full because it is calibrated as kilograms of help.
 *
 * @throws IllegalArgumentException if [bodyweightKg] or any modifier is
 *   outside [LoadLimits]. The UI validates first, so a throw here is a bug.
 */
public fun calculateEffectiveLoad(
    bodyweightKg: Double,
    profile: LoadProfile,
    modifiers: List<LoadModifier> = emptyList(),
): EffectiveLoad {
    requireInRange(bodyweightKg, LoadLimits.MIN_BODYWEIGHT_KG, LoadLimits.MAX_BODYWEIGHT_KG, "bodyweightKg")

    var addedKg = 0.0
    var assistanceKg = 0.0
    for (modifier in modifiers) {
        requireInRange(modifier.kilograms, 0.0, LoadLimits.MAX_MODIFIER_KG, "modifier.kilograms")
        when (modifier) {
            is AddedWeight -> addedKg += modifier.kilograms
            is BandAssistance -> assistanceKg += modifier.kilograms
        }
    }

    val share = profile.bodyweightShare
    return EffectiveLoad(
        fromBodyweightKg = bodyweightKg * share,
        fromAddedWeightKg = addedKg * share,
        removedByAssistanceKg = assistanceKg,
        isEstimate = profile.evidence == LoadEvidence.ESTIMATED,
    )
}
