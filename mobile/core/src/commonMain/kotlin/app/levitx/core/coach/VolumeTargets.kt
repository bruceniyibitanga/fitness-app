package app.levitx.core.coach

private const val SECONDS_PER_MINUTE = 60

/** What a movement pattern is to the person's goal. */
public enum class PatternRole {
    /** The goal's skill gates train it. */
    GOAL,

    /** Its exercises also train a goal pattern, the way rows help pull-ups. */
    SUPPORT,

    /** Neither. With no goal, every pattern is this. */
    OTHER,
}

/**
 * The hard sets per week the plan aims for on each movement pattern.
 *
 * Goal patterns get the most. In an all-round plan the rest get a proper dose.
 * In a focused plan, patterns that support the goal still do, and the others
 * get enough to hold steady. Targets shrink together when the person's week is
 * too short to fit them, so "done out of target" stays honest for someone with
 * three 20-minute sessions.
 */
public object VolumeTargets {
    /** Weekly targets for [profile]. Patterns the person has no kit to train aren't included. */
    public fun weekly(profile: CoachProfile): Map<MovementPattern, Double> {
        val ideal = MovementPattern.entries
            .filter { it in goalPatterns(profile) || hasLadderExercise(it, profile.equipment) }
            .associateWith { idealSets(roleOf(it, profile), profile.style) }
        val scale = (setsThatFit(profile) / ideal.values.sum()).coerceAtMost(1.0)
        return ideal.mapValues { it.value * scale }
    }

    /** What [pattern] is to the goal in [profile]. */
    public fun roleOf(pattern: MovementPattern, profile: CoachProfile): PatternRole {
        val goal = goalPatterns(profile)
        val supports = ExerciseCatalogue.ladder(pattern).rungs
            .any { rung -> rung.options.any { exercise -> exercise.alsoTrains.any(goal::contains) } }
        return when {
            pattern in goal -> PatternRole.GOAL
            supports -> PatternRole.SUPPORT
            else -> PatternRole.OTHER
        }
    }

    /** The patterns of the goal's gates that the person has the kit to train. */
    private fun goalPatterns(profile: CoachProfile): Set<MovementPattern> = profile.goalPath?.gates.orEmpty()
        .mapNotNull(SkillTraining::exerciseFor)
        .filter { it.fits(profile.equipment) }
        .map { it.pattern }
        .toSet()

    private fun hasLadderExercise(pattern: MovementPattern, owned: Set<Equipment>): Boolean {
        val ladder = ExerciseCatalogue.ladder(pattern)
        return ladder.usableRungAtOrBelow(ladder.rungs.lastIndex, owned) != null
    }

    private fun idealSets(role: PatternRole, style: PlanStyle): Double = when (role) {
        PatternRole.GOAL ->
            if (style ==
                PlanStyle.FOCUSED
            ) {
                CoachTuning.FOCUSED_GOAL_WEEKLY_SETS
            } else {
                CoachTuning.GOAL_PATTERN_WEEKLY_SETS
            }

        PatternRole.SUPPORT -> CoachTuning.ALL_ROUND_WEEKLY_SETS

        PatternRole.OTHER ->
            if (style == PlanStyle.FOCUSED) CoachTuning.MAINTENANCE_WEEKLY_SETS else CoachTuning.ALL_ROUND_WEEKLY_SETS
    }

    /** Roughly how many sets the person's week has room for. */
    private fun setsThatFit(profile: CoachProfile): Double {
        val workSeconds = (profile.sessionMinutes - CoachTuning.WARM_UP_MINUTES).coerceAtLeast(1) * SECONDS_PER_MINUTE
        return profile.sessionsPerWeek * workSeconds.toDouble() / CoachTuning.AVERAGE_SET_SECONDS
    }
}
