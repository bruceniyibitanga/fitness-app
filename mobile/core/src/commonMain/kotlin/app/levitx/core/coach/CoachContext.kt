package app.levitx.core.coach

import app.levitx.core.skills.Gate
import app.levitx.core.skills.SkillPath

/**
 * Everything one decision needs, worked out once from the model and the time.
 *
 * Rules, scoring terms and the readiness check all read from here, so they
 * agree on what "this week" and "rested" mean.
 */
internal class CoachContext(val model: AthleteModel, val nowEpochMillis: Long) {
    /** The goal's skill path, or null for general fitness. */
    val path: SkillPath? = model.profile.goalPath

    /** The gate being worked on, or null with no goal or once the path is done. */
    val currentGate: Gate? = model.currentGate()

    /** The pattern the current gate trains, or null with no gate the person has the kit to work on. */
    val currentGatePattern: MovementPattern? = model.currentGatePattern()

    /** The patterns the goal's gates train. */
    val goalPatterns: Set<MovementPattern> = model.goalPatterns()

    /** Weekly hard-set targets per pattern. */
    val weeklyTargets: Map<MovementPattern, Double> = VolumeTargets.weekly(model.profile)

    /** Bodyweight trend, or null with no readings. */
    val weightTrend: BodyweightTrend? = bodyweightTrend(model.weighIns, nowEpochMillis)

    /** True while fast weight loss means targets shouldn't go up. */
    val holdProgress: Boolean = weightTrend?.isFastLoss == true

    /** Reads trends from results since the person last took an easier day. */
    val trend: PerformanceTrend = PerformanceTrend(since = model.lastEasedAtEpochMillis)

    /** The last session finished by now, or null before the first. */
    val lastSession: SessionRecord? = model.lastSession(nowEpochMillis)

    private val weekStart: Long = nowEpochMillis - CoachTuning.WEEK_WINDOW_HOURS * MILLIS_PER_HOUR

    /** Hard sets toward [pattern]'s weekly target this week, counting indirect sets as a share. */
    fun weekSoFar(pattern: MovementPattern): Double = model.hardSets(pattern, weekStart, nowEpochMillis)

    /** Hard sets of [pattern]'s own exercises this week. */
    fun directThisWeek(pattern: MovementPattern): Int = model.directSets(pattern, weekStart, nowEpochMillis)

    /**
     * The most hard sets of [pattern]'s own exercises in any one of the
     * [CoachTuning.LOAD_HISTORY_WEEKS] weeks before this one.
     */
    fun recentPeak(pattern: MovementPattern): Int = (0 until CoachTuning.LOAD_HISTORY_WEEKS).maxOf { weeksBack ->
        val end = weekStart - weeksBack * DAYS_PER_WEEK * MILLIS_PER_DAY
        model.directSets(pattern, end - DAYS_PER_WEEK * MILLIS_PER_DAY, end)
    }

    /** Hours since [pattern] was last trained hard, or null when it hasn't been. */
    fun hoursSinceHard(pattern: MovementPattern): Double? = model.lastHardAt(pattern, nowEpochMillis)?.let(::hoursSince)

    /** Hours since the last session, or null before the first. */
    fun hoursSinceLastSession(): Double? = lastSession?.finishedAtEpochMillis?.let(::hoursSince)

    /** The ladder exercise the person trains for [pattern] this block, or null without the kit. */
    fun exerciseFor(pattern: MovementPattern): Exercise? = model.exerciseFor(pattern, goalPatterns)

    private fun hoursSince(epochMillis: Long): Double = (nowEpochMillis - epochMillis).toDouble() / MILLIS_PER_HOUR
}

/**
 * The ladder exercise this person trains for [pattern] in block [blockIndex].
 *
 * An exercise the person chose ([AthleteModel.preferred]) wins while it is
 * an option on the rung they train at, with their kit. Otherwise goal
 * patterns use the first option on the rung, because skills need repetition,
 * and other patterns rotate through the rung's options block by block.
 * Planning and progression both pick through here, so they can't disagree.
 */
internal fun AthleteModel.exerciseFor(
    pattern: MovementPattern,
    goalPatterns: Set<MovementPattern>,
    blockIndex: Int = block.index,
): Exercise? {
    val ladder = ExerciseCatalogue.ladder(pattern)
    val rung = ladder.usableRungAtOrBelow(rung(pattern), profile.equipment) ?: return null
    val chosen = preferred[pattern]?.let { id -> ladder.optionsAt(rung, profile.equipment).firstOrNull { it.id == id } }
    val variant = if (pattern in goalPatterns) 0 else blockIndex
    return chosen ?: ladder.exerciseAt(rung, profile.equipment, variant)
}

/**
 * The exercises this person could train for [pattern] at the rung they're
 * on, with their kit, the one they train now included. One or none means
 * there's nothing to swap to.
 */
public fun AthleteModel.swapOptions(pattern: MovementPattern): List<Exercise> {
    if (!pattern.isStrength) return emptyList()
    val ladder = ExerciseCatalogue.ladder(pattern)
    return ladder.usableRungAtOrBelow(rung(pattern), profile.equipment)
        ?.let { ladder.optionsAt(it, profile.equipment) }
        .orEmpty()
}

/**
 * The person chose to train [exercise] for its movement. It stays their
 * pick while it is an option on the rung they train at; moving up a rung
 * lets the coach choose again. Unchanged when [exercise] isn't one of
 * [swapOptions], for example because the rung moved since it was offered.
 */
public fun AthleteModel.withPreferred(exercise: Exercise): AthleteModel =
    if (swapOptions(exercise.pattern).any { it.id == exercise.id }) {
        copy(preferred = preferred + (exercise.pattern to exercise.id))
    } else {
        this
    }

/**
 * The ladder exercise this person trains for [pattern] right now, or null
 * without the kit for any of it. For the pattern of the gate being worked on,
 * it is what the ladder holds for after the gate.
 */
public fun AthleteModel.currentExercise(pattern: MovementPattern): Exercise? = exerciseFor(pattern, goalPatterns())

/** The gate being worked on, or null with no goal or once the path is done. */
public fun AthleteModel.currentGate(): Gate? =
    profile.goalPath?.let { path -> path.progressFor(gateBests).currentIndex?.let(path.gates::get) }

/**
 * The pattern trained through the current gate rather than its ladder, or
 * null with no gate to work on. Sessions train this pattern with the gate's
 * own exercise until the gate is passed.
 */
public fun AthleteModel.currentGatePattern(): MovementPattern? =
    currentGate()?.let(SkillTraining::exerciseFor)?.takeIf { it.fits(profile.equipment) }?.pattern

/**
 * Kit the current gate needs that the person hasn't said they have. Empty
 * with no gate, or with the kit. Without it the gate can't be trained, so the
 * apps say what's missing instead of quietly leaving the goal out.
 */
public fun AthleteModel.kitMissingForGoal(): Set<Equipment> =
    currentGate()?.let(SkillTraining::exerciseFor)?.equipment?.minus(profile.equipment).orEmpty()

/** The patterns the goal's gates train, which get the most weekly volume. Empty with no goal. */
internal fun AthleteModel.goalPatterns(): Set<MovementPattern> =
    profile.goalPath?.let(SkillTraining::patternsOf).orEmpty()
