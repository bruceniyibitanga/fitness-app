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
    val currentGate: Gate? = path?.let { goal -> goal.progressFor(model.gateBests).currentIndex?.let(goal.gates::get) }

    /** The pattern the current gate trains, or null with no gate to work on. */
    val currentGatePattern: MovementPattern? = currentGate?.let(SkillTraining::exerciseFor)?.pattern

    /** The patterns the goal's gates train. */
    val goalPatterns: Set<MovementPattern> = path?.let(SkillTraining::patternsOf).orEmpty()

    /** Weekly hard-set targets per pattern. */
    val weeklyTargets: Map<MovementPattern, Double> = VolumeTargets.weekly(model.profile)

    /** Bodyweight trend, or null with no readings. */
    val weightTrend: BodyweightTrend? = bodyweightTrend(model.weighIns, nowEpochMillis)

    /** True while fast weight loss means targets shouldn't go up. */
    val holdProgress: Boolean = weightTrend?.isFastLoss == true

    /** Reads trends from results since the person last took an easier day. */
    val trend: PerformanceTrend = PerformanceTrend(since = model.lastEasedAtEpochMillis)

    /** The last finished session, or null before the first. */
    val lastSession: SessionRecord? = model.history.lastOrNull()

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
    fun hoursSinceHard(pattern: MovementPattern): Double? = model.lastHardAt(pattern)?.let(::hoursSince)

    /** Hours since the last session, or null before the first. */
    fun hoursSinceLastSession(): Double? = model.lastSessionAtEpochMillis?.let(::hoursSince)

    /** The ladder exercise the person trains for [pattern] this block, or null without the kit. */
    fun exerciseFor(pattern: MovementPattern): Exercise? = model.exerciseFor(pattern, goalPatterns)

    private fun hoursSince(epochMillis: Long): Double = (nowEpochMillis - epochMillis).toDouble() / MILLIS_PER_HOUR
}

/**
 * The ladder exercise this person trains for [pattern] in block [blockIndex].
 *
 * Goal patterns always use the first option on the rung, because skills need
 * repetition. Other patterns rotate through the rung's options block by block.
 * Planning and progression both pick through here, so they can't disagree.
 */
internal fun AthleteModel.exerciseFor(
    pattern: MovementPattern,
    goalPatterns: Set<MovementPattern>,
    blockIndex: Int = block.index,
): Exercise? {
    val ladder = ExerciseCatalogue.ladder(pattern)
    val variant = if (pattern in goalPatterns) 0 else blockIndex
    return ladder.usableRungAtOrBelow(rung(pattern), profile.equipment)
        ?.let { ladder.exerciseAt(it, profile.equipment, variant) }
}
