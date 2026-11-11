package app.levitx.core.coach

import kotlin.math.abs

private const val SECONDS_PER_MINUTE = 60

/** A hard set on a goal pattern that isn't the skill work itself counts this much toward the goal. */
private const val GOAL_SUPPORT_SHARE = 0.5

/** The things a session is scored on. */
internal enum class Term {
    GOAL,
    VOLUME,
    RECOVERY,
    TIME,
    PREFERENCE,
    NOVELTY,
}

/**
 * One thing a session is scored on. Terms are independent of each other, so a
 * new one is added to [ScoringTerms.all] without changing the rest.
 */
internal interface ScoringTerm {
    /** Which term this is, for explaining the choice. */
    val term: Term

    /** How much the term counts. Weights add up to 1. */
    val weight: Double

    /** How well [session] does on this term, from 0 to 1. */
    fun score(session: PlannedSession, context: CoachContext): Double
}

/** How much of the session's hard work moves the goal forward. */
internal object GoalTerm : ScoringTerm {
    override val term: Term = Term.GOAL
    override val weight: Double = CoachTuning.WEIGHT_GOAL

    override fun score(session: PlannedSession, context: CoachContext): Double {
        val toward = session.plans.filter { it.isHard }.sumOf { plan ->
            when {
                plan.gateId != null -> plan.sets.size.toDouble()
                plan.exercise.pattern in context.goalPatterns -> plan.sets.size * GOAL_SUPPORT_SHARE
                else -> 0.0
            }
        }
        return (toward / CoachTuning.GOAL_SETS_FOR_FULL_SCORE).coerceAtMost(1.0)
    }
}

/**
 * How much of the week's remaining volume the session covers. Weekly volume
 * has a dose-response with diminishing returns (Pelland et al. 2026), so sets
 * past a pattern's target earn nothing.
 */
internal object VolumeTerm : ScoringTerm {
    override val term: Term = Term.VOLUME
    override val weight: Double = CoachTuning.WEIGHT_VOLUME

    override fun score(session: PlannedSession, context: CoachContext): Double {
        val closed = session.hardSets.entries.sumOf { (pattern, sets) -> sets.coerceAtMost(gap(pattern, context)) }
        val closable = context.weeklyTargets.keys.sumOf { gap(it, context) }.coerceAtMost(setsThatFit(context))
        return if (closable <= 0.0) 0.0 else (closed / closable).coerceAtMost(1.0)
    }

    /** Hard sets [pattern] is still short of its weekly target. */
    fun gap(pattern: MovementPattern, context: CoachContext): Double =
        ((context.weeklyTargets[pattern] ?: 0.0) - context.weekSoFar(pattern)).coerceAtLeast(0.0)

    private fun setsThatFit(context: CoachContext): Double {
        val minutes = context.model.profile.sessionMinutes - CoachTuning.WARM_UP_MINUTES
        return minutes.coerceAtLeast(1) * SECONDS_PER_MINUTE.toDouble() / CoachTuning.AVERAGE_SET_SECONDS
    }
}

/** How rested the patterns trained hard are. Light work doesn't need rest, so it counts as rested. */
internal object RecoveryTerm : ScoringTerm {
    override val term: Term = Term.RECOVERY
    override val weight: Double = CoachTuning.WEIGHT_RECOVERY

    override fun score(session: PlannedSession, context: CoachContext): Double =
        session.plans.map { if (it.isHard) rested(it.exercise.pattern, context) else 1.0 }.average()

    /** How rested [pattern] is, from 0 just after hard work to 1 at [CoachTuning.FULL_RECOVERY_HOURS]. */
    fun rested(pattern: MovementPattern, context: CoachContext): Double =
        context.hoursSinceHard(pattern)?.let { (it / CoachTuning.FULL_RECOVERY_HOURS).coerceIn(0.0, 1.0) } ?: 1.0
}

/** How close the session comes to the time the person has. */
internal object TimeTerm : ScoringTerm {
    override val term: Term = Term.TIME
    override val weight: Double = CoachTuning.WEIGHT_TIME

    override fun score(session: PlannedSession, context: CoachContext): Double {
        val available = context.model.profile.sessionMinutes.toDouble()
        return (1 - abs(available - session.estimatedMinutes) / available).coerceIn(0.0, 1.0)
    }
}

/** A small nudge away from exercises the person keeps skipping. */
internal object PreferenceTerm : ScoringTerm {
    override val term: Term = Term.PREFERENCE
    override val weight: Double = CoachTuning.WEIGHT_PREFERENCE

    override fun score(session: PlannedSession, context: CoachContext): Double {
        val disliked = session.plans.count {
            (context.model.skips[it.exercise.id] ?: 0) >= CoachTuning.SKIPS_BEFORE_DISLIKED
        }
        return 1 - disliked.toDouble() / session.plans.size
    }
}

/**
 * How different the session is from the last one. Planned variation helps and
 * random variation hurts (Kassiano et al. 2022), so this has the smallest
 * weight and can only settle a choice between sessions that are otherwise even.
 */
internal object NoveltyTerm : ScoringTerm {
    override val term: Term = Term.NOVELTY
    override val weight: Double = CoachTuning.WEIGHT_NOVELTY

    override fun score(session: PlannedSession, context: CoachContext): Double {
        val last = context.lastSession?.exerciseIds.orEmpty().toSet()
        val now = session.plans.map { it.exercise.id }.toSet()
        return if (last.isEmpty()) 0.0 else 1 - (now intersect last).size.toDouble() / (now union last).size
    }
}

/** The terms the coach scores with. */
internal object ScoringTerms {
    val all: List<ScoringTerm> = listOf(GoalTerm, VolumeTerm, RecoveryTerm, TimeTerm, PreferenceTerm, NoveltyTerm)
}

/**
 * A session with its score.
 *
 * @property parts Each term's weighted share of the score.
 */
internal data class ScoredSession(val session: PlannedSession, val parts: Map<Term, Double>) {
    /** The whole score. */
    val total: Double = parts.values.sum()
}

/**
 * Scores sessions as the weighted sum of [terms]. There is no randomness: the
 * same model at the same time always scores a session the same.
 */
internal class SessionScorer(private val terms: List<ScoringTerm> = ScoringTerms.all) {
    /** Scores [session] for [context]. */
    fun score(session: PlannedSession, context: CoachContext): ScoredSession =
        ScoredSession(session, terms.associate { it.term to it.weight * it.score(session, context) })
}
