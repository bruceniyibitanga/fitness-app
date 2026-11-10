package app.levitx.core.strength

import app.levitx.core.requireInRange

/**
 * Most reps (performed plus reps in reserve) the estimate is trusted for.
 *
 * Rep-based prediction equations were validated against sets of 10 reps or
 * fewer (LeSuer et al., 1997), so longer sets return null rather than a number
 * that looks precise but isn't.
 */
public const val MAX_REPS_FOR_ONE_REP_MAX_ESTIMATE: Int = 10

/** Heaviest load accepted, in kilograms. Anything above is a typo. */
private const val MAX_LOAD_KG = 1000.0

/** Most reps accepted for one set. */
private const val MAX_REPS = 1000

/** Most reps in reserve accepted. RIR scales stop being meaningful well before this. */
private const val MAX_REPS_IN_RESERVE = 20

/** The Epley formula divides reps by 30. */
private const val EPLEY_REP_DIVISOR = 30.0

/**
 * Estimates a one-rep max with the Epley formula: load × (1 + reps / 30).
 *
 * [repsInReserve] is how many more reps the person could have done. A set of 5
 * with 2 in reserve is treated like a set of 7 to failure, which is how
 * reps-in-reserve scales are normally used.
 *
 * @return [loadKg] unchanged for a single rep to failure, or null when the
 *   total is above [MAX_REPS_FOR_ONE_REP_MAX_ESTIMATE].
 * @throws IllegalArgumentException for negative or non-finite input, or fewer
 *   than one rep.
 */
public fun estimateOneRepMax(loadKg: Double, reps: Int, repsInReserve: Int = 0): Double? {
    requireInRange(loadKg, 0.0, MAX_LOAD_KG, "loadKg")
    requireInRange(reps, 1, MAX_REPS, "reps")
    requireInRange(repsInReserve, 0, MAX_REPS_IN_RESERVE, "repsInReserve")

    val repsToFailure = reps + repsInReserve
    return when {
        repsToFailure > MAX_REPS_FOR_ONE_REP_MAX_ESTIMATE -> null
        repsToFailure == 1 -> loadKg
        else -> loadKg * (1 + repsToFailure / EPLEY_REP_DIVISOR)
    }
}
