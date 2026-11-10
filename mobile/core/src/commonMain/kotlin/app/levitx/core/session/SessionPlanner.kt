package app.levitx.core.session

import app.levitx.core.skills.Gate
import app.levitx.core.skills.GateMeasure
import kotlin.math.roundToInt

/**
 * One set to do.
 *
 * @property target Reps or seconds to aim for, in the gate's measure.
 * @property isBestEffort True for the last set, which is done to near failure
 *   to find out whether the person has improved.
 */
public data class PlannedSet(val target: Int, val isBestEffort: Boolean)

/**
 * Today's session for one gate.
 *
 * @property gate The gate being trained.
 * @property sets Work sets first, best-effort set last.
 * @property restSeconds Rest between sets.
 * @property repsInReserve How many reps to leave in the tank on work sets.
 */
public data class SessionPlan(
    val gate: Gate,
    val sets: List<PlannedSet>,
    val restSeconds: Int,
    val repsInReserve: Int,
)

/**
 * Builds a session from the person's current best at a gate.
 *
 * The shape is a common submaximal approach: a few work sets at about 70% of
 * the current best, stopping with reps in reserve, then one best-effort set.
 * Work sets build volume without grinding to failure every set; the last set
 * shows progress, and its result becomes the new best when it beats the old one.
 *
 * Rest follows the usual strength guidance of 2 minutes or more between
 * multi-joint sets, shorter for holds.
 *
 * These numbers are a starting point and must be reviewed by a qualified
 * coach before release, like the gates themselves.
 */
public object SessionPlanner {
    /** Work sets before the best-effort set. */
    public const val WORK_SETS: Int = 3

    /** Work sets aim for this share of the current best. */
    public const val WORK_FRACTION: Double = 0.7

    /** Shortest hold worth doing as a set, in seconds. */
    public const val MIN_HOLD_SECONDS: Int = 5

    /** Rest between rep sets. */
    public const val REST_SECONDS_REPS: Int = 120

    /** Rest between holds. */
    public const val REST_SECONDS_HOLDS: Int = 90

    /** Reps to leave in reserve on work sets. */
    public const val TARGET_REPS_IN_RESERVE: Int = 2

    /**
     * Plans a session for [gate] given the person's [best] result there.
     *
     * @throws IllegalArgumentException if [best] is negative.
     */
    public fun planFor(gate: Gate, best: Int): SessionPlan {
        require(best >= 0) { "best must not be negative" }
        val isHold = gate.measure == GateMeasure.HOLD_SECONDS
        val minimum = if (isHold) MIN_HOLD_SECONDS else 1
        val workTarget = maxOf(minimum, (best * WORK_FRACTION).roundToInt())
        // Aim one past the current best, without going past the gate itself.
        val bestEffortTarget = (best + 1).coerceAtLeast(minimum).coerceAtMost(gate.target)
        val sets = List(WORK_SETS) { PlannedSet(workTarget, isBestEffort = false) } +
            PlannedSet(bestEffortTarget, isBestEffort = true)
        return SessionPlan(
            gate = gate,
            sets = sets,
            restSeconds = if (isHold) REST_SECONDS_HOLDS else REST_SECONDS_REPS,
            repsInReserve = TARGET_REPS_IN_RESERVE,
        )
    }
}
