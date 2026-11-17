package app.levitx.core.coach

import app.levitx.core.skills.GateMeasure

/**
 * What the ladder work is tuned for, alongside any skill goal. It changes how
 * long to rest, how close to failure to go, how much weekly volume to aim for
 * and where in each exercise's rep range the person moves on.
 *
 * The bottom of every range stays the same, so a target stored under one aim
 * is still valid under another and switching never loses progress.
 *
 * Sources, in COACH-ENGINE.md:
 * - ACSM 2009 progression models: 2 to 3 minutes' rest and lower reps for
 *   strength, 1 to 2 minutes for growth, under a minute to 2 for endurance.
 * - Schoenfeld et al. 2016: 3 minutes' rest gave more strength and growth than 1.
 * - Schoenfeld, Ogborn and Krieger 2017: more weekly sets, more growth.
 * - Schoenfeld et al. 2017: low and high loads grow muscle alike when taken
 *   close to failure, but heavier work builds more strength.
 * - Robinson et al. 2024: growth improves closer to failure; strength doesn't need it.
 *
 * @property restSeconds Rest between hard sets of reps.
 * @property holdRestSeconds Rest between hard holds.
 * @property repsInReserve Reps to leave in the tank on each work set.
 * @property volumeShare Weekly hard sets as a share of the balanced plan's.
 */
public enum class TrainingAim(
    public val restSeconds: Int,
    public val holdRestSeconds: Int,
    public val repsInReserve: Int,
    public val volumeShare: Double,
) {
    /** Strength, muscle and staying power in equal measure. The default. */
    BALANCED(restSeconds = 120, holdRestSeconds = 90, repsInReserve = 2, volumeShare = 1.0),

    /** More weekly sets, taken closer to failure. */
    MUSCLE(restSeconds = 120, holdRestSeconds = 90, repsInReserve = 1, volumeShare = 1.33),

    /** Harder variations sooner, at lower reps, with longer rest. */
    STRENGTH(restSeconds = 180, holdRestSeconds = 120, repsInReserve = 2, volumeShare = 1.0),

    /** More reps before moving on, with short rest. */
    CONDITIONING(restSeconds = 60, holdRestSeconds = 45, repsInReserve = 2, volumeShare = 1.0),
    ;

    /**
     * The target at which [exercise] has been beaten and the person moves on:
     * weight, a harder rung, or more reps at the top of the ladder.
     *
     * - Strength: the middle of the range, so heavier work comes sooner.
     * - Conditioning: half the range again past its top, up to the ceiling.
     * - Otherwise the top of the range.
     *
     * Never below the bottom of the range or above the exercise's ceiling.
     */
    public fun top(exercise: Exercise): Int {
        val range = exercise.range
        val span = range.last - range.first
        return when (this) {
            STRENGTH -> range.first + (span + 1) / 2
            CONDITIONING -> (range.last + span / 2).coerceAtMost(exercise.ceiling)
            BALANCED, MUSCLE -> range.last
        }.coerceIn(range.first, exercise.ceiling)
    }

    /** Rest between hard sets of [exercise]. */
    public fun restFor(exercise: Exercise): Int =
        if (exercise.measure == GateMeasure.HOLD_SECONDS) holdRestSeconds else restSeconds

    /** Roughly how long one hard set takes with its rest, for fitting a week's sets into the time. */
    internal val averageSetSeconds: Int
        get() = WORK_SECONDS_PER_SET + restSeconds
}

/** Roughly how long the work of a hard set takes, before its rest. */
private const val WORK_SECONDS_PER_SET = 30
