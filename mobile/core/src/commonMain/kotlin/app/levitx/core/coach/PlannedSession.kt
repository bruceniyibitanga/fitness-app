package app.levitx.core.coach

import app.levitx.core.session.PlannedSet
import app.levitx.core.skills.GateMeasure
import kotlin.math.roundToInt

private const val SECONDS_PER_MINUTE = 60

/** How hard an exercise is trained in a session. */
public enum class Intensity {
    /** Close enough to the person's limit to drive progress. These sets count toward weekly volume. */
    HARD,

    /** Well inside what they can do: fewer sets, further from failure. Doesn't count as hard work. */
    LIGHT,
}

/**
 * One exercise in a session, with its sets.
 *
 * @property exercise What to do.
 * @property sets The sets, in order. A best-effort set, if there is one, comes
 *   last, except in a gate attempt, where the attempt comes first.
 * @property restSeconds Rest between sets.
 * @property repsInReserve Reps to leave in the tank on the work sets.
 * @property intensity Whether this is hard or light work.
 * @property gateId The skill gate this trains, when it is the session's skill work.
 * @property isGateAttempt True when the first set is a fresh attempt at the
 *   gate's full target, before the usual work sets.
 * @throws IllegalArgumentException if [sets] is empty, or for an attempt that
 *   isn't skill work.
 */
public data class ExercisePlan(
    val exercise: Exercise,
    val sets: List<PlannedSet>,
    val restSeconds: Int,
    val repsInReserve: Int,
    val intensity: Intensity,
    val gateId: String? = null,
    val isGateAttempt: Boolean = false,
) {
    init {
        require(sets.isNotEmpty()) { "an exercise needs at least one set" }
        require(!isGateAttempt || (gateId != null && sets.first().isBestEffort)) {
            "a gate attempt is skill work that starts with the attempt"
        }
    }

    /** True for hard work. */
    val isHard: Boolean
        get() = intensity == Intensity.HARD

    /** The target of the work sets, or of the only set when there are no work sets. */
    val workTarget: Int
        get() = (sets.firstOrNull { !it.isBestEffort } ?: sets.first()).target

    /** Roughly how long the exercise takes, rests included, in seconds. */
    val estimatedSeconds: Int
        get() = sets.sumOf { workSeconds(it) + restSeconds }

    /** Hard sets this plan adds per pattern. Empty for light work. */
    val hardSets: Map<MovementPattern, Double>
        get() = if (isHard) exercise.setsByPattern(sets.size) else emptyMap()

    /**
     * The same exercise as light work: no best-effort set and no gate
     * attempt, at most [CoachTuning.LIGHT_SETS_PER_EXERCISE] sets, a lower
     * target and shorter rest.
     */
    public fun lightened(): ExercisePlan {
        val target = (workTarget * CoachTuning.LIGHT_TARGET_FRACTION).roundToInt().coerceAtLeast(1)
        return copy(
            sets = List(CoachTuning.LIGHT_SETS_PER_EXERCISE) { PlannedSet(target, isBestEffort = false) },
            restSeconds = CoachTuning.LIGHT_REST_SECONDS,
            repsInReserve = CoachTuning.LIGHT_REPS_IN_RESERVE,
            intensity = Intensity.LIGHT,
            isGateAttempt = false,
        )
    }

    /** The same plan cut to its first [count] sets. Does nothing when it is already that short. */
    public fun trimmedTo(count: Int): ExercisePlan = copy(sets = sets.take(count.coerceAtLeast(1)))

    private fun workSeconds(set: PlannedSet): Int {
        val isHold = exercise.measure == GateMeasure.HOLD_SECONDS
        val once = if (isHold) set.target else set.target * CoachTuning.SECONDS_PER_REP
        return if (exercise.isPerSide) once * 2 else once
    }
}

/**
 * A whole session as the coach planned it.
 *
 * @property plans The exercises, in the order to do them.
 * @throws IllegalArgumentException if [plans] is empty.
 */
public data class PlannedSession(val plans: List<ExercisePlan>) {
    init {
        require(plans.isNotEmpty()) { "a session needs at least one exercise" }
    }

    /** Roughly how long the session takes, warm-up included, in minutes. */
    val estimatedMinutes: Int
        get() = CoachTuning.WARM_UP_MINUTES +
            (plans.sumOf { it.estimatedSeconds } + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE

    /** Hard sets the session adds per pattern, counting indirect sets as a share. */
    val hardSets: Map<MovementPattern, Double>
        get() = plans.map { it.hardSets }.sumByPattern()

    /** Hard sets of each pattern's own exercises. This is what the safety limits count. */
    val directHardSets: Map<MovementPattern, Int>
        get() = plans.filter { it.isHard }.groupingBy { it.exercise.pattern }.fold(0) { sets, plan ->
            sets +
                plan.sets.size
        }

    /** True when nothing in the session is hard work. */
    val isLight: Boolean
        get() = plans.none { it.isHard }

    /** The same exercises as a lighter session. */
    public fun lightened(): PlannedSession = PlannedSession(plans.map { it.lightened() })
}

/** Adds up sets per pattern across several counts. */
internal fun List<Map<MovementPattern, Double>>.sumByPattern(): Map<MovementPattern, Double> = buildMap {
    this@sumByPattern.forEach { counts ->
        counts.forEach { (pattern, sets) -> put(pattern, (get(pattern) ?: 0.0) + sets) }
    }
}
