package app.levitx.core.coach

import app.levitx.core.pain.PainAdvice

/**
 * Why a session can't be done as it stands, and what would fix it.
 *
 * Each kind knows its own fix, so adding a rule never means touching the code
 * that repairs sessions.
 */
internal sealed interface Blocked {
    /** The same session with the problem eased or removed, or null when nothing would be left. */
    fun relax(session: PlannedSession): PlannedSession?

    /** [area] is sore. Ease off hard work that loads it, or with [stop], leave it out altogether. */
    data class Pain(val area: BodyArea, val stop: Boolean) : Blocked {
        override fun relax(session: PlannedSession): PlannedSession? = if (stop) {
            session.keeping { area !in it.exercise.pattern.loads }
        } else {
            session.lightening { area in it.exercise.pattern.loads }
        }
    }

    /** [pattern] was trained hard too recently. */
    data class Recovering(val pattern: MovementPattern) : Blocked {
        override fun relax(session: PlannedSession): PlannedSession? = session.lightening {
            it.exercise.pattern ==
                pattern
        }
    }

    /** [pattern] is at its weekly limit. */
    data class VolumeCapped(val pattern: MovementPattern) : Blocked {
        override fun relax(session: PlannedSession): PlannedSession? =
            session.lightening { it.exercise.pattern == pattern }
    }

    /** The session trains gate [gateId], which isn't the one the person is on. */
    data class GateOutOfOrder(val gateId: String) : Blocked {
        override fun relax(session: PlannedSession): PlannedSession? = session.keeping { it.gateId != gateId }
    }
}

/** A safety rule. It is never traded off against anything: a session that breaks one isn't offered. */
internal fun interface HardRule {
    /** Why [session] can't be done, or null when this rule has no objection. */
    fun blocks(session: PlannedSession, context: CoachContext): Blocked?
}

/**
 * Pain, from the pain-monitoring guidance in `core.pain`. Advice to see a
 * professional takes everything that loads the area out of the plan. Advice to
 * ease off allows light work only.
 */
internal object PainRule : HardRule {
    override fun blocks(session: PlannedSession, context: CoachContext): Blocked? =
        session.plans.firstNotNullOfOrNull { plan ->
            plan.exercise.pattern.loads.firstNotNullOfOrNull { area ->
                when (context.model.pain[area]) {
                    PainAdvice.SEE_PROFESSIONAL -> Blocked.Pain(area, stop = true)
                    PainAdvice.EASE_OFF -> Blocked.Pain(area, stop = false).takeIf { plan.isHard }
                    else -> null
                }
            }
        }
}

/** No hard work for a pattern within [CoachTuning.MIN_HOURS_BETWEEN_HARD_SESSIONS] of the last. */
internal object RecoveryRule : HardRule {
    override fun blocks(session: PlannedSession, context: CoachContext): Blocked? = session.plans
        .filter { it.isHard }
        .map { it.exercise.pattern }
        .firstOrNull { pattern ->
            val hours = context.hoursSinceHard(pattern)
            hours != null && hours < CoachTuning.MIN_HOURS_BETWEEN_HARD_SESSIONS
        }
        ?.let(Blocked::Recovering)
}

/**
 * Weekly hard sets of a pattern's own exercises stay under a ceiling, and can
 * only grow so fast from one week to the next. Tendons adapt more slowly than
 * muscle (Mersmann et al. 2017), so this keeps strength from running ahead of
 * what they tolerate.
 */
internal object VolumeCapRule : HardRule {
    override fun blocks(session: PlannedSession, context: CoachContext): Blocked? = session.directHardSets.entries
        .firstOrNull { (pattern, sets) -> context.directThisWeek(pattern) + sets > allowed(pattern, context) }
        ?.let { Blocked.VolumeCapped(it.key) }

    /** Hard sets of [pattern]'s own exercises it may reach this week. */
    fun allowed(pattern: MovementPattern, context: CoachContext): Double {
        val grown = context.recentPeak(pattern) * (1 + CoachTuning.MAX_WEEKLY_INCREASE)
        return grown.coerceIn(CoachTuning.STARTING_WEEKLY_HARD_SETS, CoachTuning.MAX_WEEKLY_HARD_SETS)
    }
}

/** Skill gates are trained in order: only the gate the person is on. */
internal object GateOrderRule : HardRule {
    override fun blocks(session: PlannedSession, context: CoachContext): Blocked? = session.plans
        .mapNotNull { it.gateId }
        .firstOrNull { it != context.currentGate?.id }
        ?.let(Blocked::GateOutOfOrder)
}

/**
 * A session that passes every rule.
 *
 * @property relaxed What had to be eased or removed to get there, in order. Empty when nothing did.
 */
internal data class Repaired(val session: PlannedSession, val relaxed: List<Blocked>)

/** The rules every session must pass, and the repair that makes a session pass them. */
internal object HardRules {
    val all: List<HardRule> = listOf(PainRule, GateOrderRule, RecoveryRule, VolumeCapRule)

    /** The first objection any of [rules] has to [session], or null when it is safe. */
    fun firstBlock(session: PlannedSession, context: CoachContext, rules: List<HardRule> = all): Blocked? =
        rules.firstNotNullOfOrNull { it.blocks(session, context) }

    /**
     * The nearest safe version of [session]: each objection is eased or removed
     * in turn until none is left. Null when nothing safe remains.
     *
     * Every fix makes a hard exercise light or takes one out, and no rule
     * objects to more light work, so this always ends.
     */
    fun repair(session: PlannedSession, context: CoachContext, rules: List<HardRule> = all): Repaired? {
        var current: PlannedSession? = session
        val relaxed = mutableListOf<Blocked>()
        var blocked = firstBlock(session, context, rules)
        while (current != null && blocked != null) {
            relaxed += blocked
            current = blocked.relax(current)
            blocked = current?.let { firstBlock(it, context, rules) }
        }
        return current?.let { Repaired(it, relaxed) }
    }
}

/** The session with only the plans [keep] accepts, or null when none are left. */
private fun PlannedSession.keeping(keep: (ExercisePlan) -> Boolean): PlannedSession? =
    plans.filter(keep).takeIf { it.isNotEmpty() }?.let(::PlannedSession)

/** The session with the hard plans [matching] made light, or null when there were none to change. */
private fun PlannedSession.lightening(matching: (ExercisePlan) -> Boolean): PlannedSession? {
    val eased = plans.map { if (it.isHard && matching(it)) it.lightened() else it }
    return PlannedSession(eased).takeIf { eased != plans }
}
