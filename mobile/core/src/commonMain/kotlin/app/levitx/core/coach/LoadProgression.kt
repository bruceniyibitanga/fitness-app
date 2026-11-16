package app.levitx.core.coach

import app.levitx.core.load.LoadKit

/** Weights closer than this are the same weight: kit is weighed to the gram. */
private const val SAME_WEIGHT_KG = 0.005

/**
 * How much weight to add to an exercise, from the kit the person owns.
 *
 * Steps are sized against the whole load a rep moves, not just the added
 * weight: 2.5 kg is a 3% step on an 80 kg person's pull-up, and a 10% step on
 * a 40 kg push-up. ACSM's progression model puts a step at 2 to 10% of the
 * load once the top of the rep range is beaten (ACSM 2009). The coach takes
 * the lightest weight the kit can make that is at least the bottom of that,
 * and none at all if the kit's next step is over the top of it.
 */
internal object LoadProgression {
    /**
     * The next weight up for [exercise] from [currentKg], or null when the kit
     * can't make a step of the right size or the weight would pass
     * [CoachTuning.MAX_ADDED_SHARE_OF_BODYWEIGHT].
     *
     * @param bodyweightKg The person's weight, or null if they haven't entered it.
     */
    fun nextUp(exercise: Exercise, currentKg: Double, kit: LoadKit, bodyweightKg: Double?): Double? {
        if (exercise.carriers.isEmpty()) return null
        val bodyweight = bodyweightKg ?: CoachTuning.REFERENCE_BODYWEIGHT_KG
        val moved = movedKg(exercise, bodyweight, currentKg)
        val heaviest = bodyweight * CoachTuning.MAX_ADDED_SHARE_OF_BODYWEIGHT
        val shareOfAdded = exercise.loadProfile?.addedWeightShare ?: 1.0
        val next = kit.choices(exercise.carriers)
            .filter { it > currentKg + SAME_WEIGHT_KG && it <= heaviest + SAME_WEIGHT_KG }
            .firstOrNull { (it - currentKg) * shareOfAdded / moved >= CoachTuning.MIN_LOAD_STEP_FRACTION }
        return next?.takeIf { (it - currentKg) * shareOfAdded / moved <= CoachTuning.MAX_LOAD_STEP_FRACTION }
    }

    /** The next weight down from [currentKg] the kit can make, or 0 for bodyweight. */
    fun nextDown(exercise: Exercise, currentKg: Double, kit: LoadKit): Double =
        kit.choices(exercise.carriers).lastOrNull { it < currentKg - SAME_WEIGHT_KG } ?: 0.0

    /**
     * The weight to prescribe for [wantedKg] with the kit the person has now:
     * the heaviest it can make that isn't over it. Kit that has gone (a vest
     * sold, plates lent) brings the weight down rather than asking for more.
     */
    fun available(exercise: Exercise, wantedKg: Double, kit: LoadKit): Double = if (wantedKg <= 0.0) {
        0.0
    } else {
        kit.choices(exercise.carriers).lastOrNull { it <= wantedKg + SAME_WEIGHT_KG } ?: 0.0
    }

    /**
     * Kilograms one rep moves: the exercise's share of bodyweight plus its
     * share of the added weight. Exercises without a measured or reasoned
     * share are sized as if all of bodyweight moved, which is close for squats
     * and lunges, the main ones that take weight without a share.
     */
    private fun movedKg(exercise: Exercise, bodyweightKg: Double, addedKg: Double): Double {
        val profile = exercise.loadProfile
        return if (profile == null) {
            bodyweightKg + addedKg
        } else {
            bodyweightKg * profile.bodyweightShare + addedKg * profile.addedWeightShare
        }
    }
}
