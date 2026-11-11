package app.levitx.core.coach

import app.levitx.core.requireInRange
import app.levitx.core.skills.SkillCatalogue
import app.levitx.core.skills.SkillPath

/** How much of the plan goes to the goal skill. */
public enum class PlanStyle {
    /** The goal skill first, then proper work for the rest of the body. The default. */
    ALL_ROUND,

    /** Mostly the goal skill. Everything else gets just enough to hold steady. */
    FOCUSED,
}

/** The setup answer to "Can you do a strict pull-up?", which picks a safe first pulling exercise. */
public enum class PullUpAnswer {
    /** They can. */
    YES,

    /** They can't yet. */
    NOT_YET,

    /** They don't know. Treated like [NOT_YET], the safe side. */
    NOT_SURE,
}

/**
 * What the person told the coach at setup. Everything else the coach learns
 * from training.
 *
 * Training days are not here on purpose. They drive reminders in the apps; the
 * plan itself is a rolling queue and only needs to know how many sessions a
 * week to aim for.
 *
 * @property goalPathId The skill they are working toward, as a [SkillPath.id],
 *   or null for general fitness with no skill goal.
 * @property style How much of the plan goes to the goal.
 * @property sessionsPerWeek Sessions a week they plan to do.
 * @property sessionMinutes Minutes they have for a session.
 * @property equipment Kit they have told us about.
 * @throws IllegalArgumentException if [goalPathId] isn't a known path, or a
 *   number is outside its limits.
 */
public data class CoachProfile(
    val goalPathId: String?,
    val style: PlanStyle = PlanStyle.ALL_ROUND,
    val sessionsPerWeek: Int = DEFAULT_SESSIONS_PER_WEEK,
    val sessionMinutes: Int = DEFAULT_SESSION_MINUTES,
    val equipment: Set<Equipment> = emptySet(),
) {
    init {
        require(goalPathId == null || SkillCatalogue.byId(goalPathId) != null) { "goalPathId must be a known path" }
        requireInRange(sessionsPerWeek, MIN_SESSIONS_PER_WEEK, MAX_SESSIONS_PER_WEEK, "sessionsPerWeek")
        requireInRange(sessionMinutes, MIN_SESSION_MINUTES, MAX_SESSION_MINUTES, "sessionMinutes")
    }

    /** The goal's skill path, or null for general fitness. */
    val goalPath: SkillPath?
        get() = goalPathId?.let(SkillCatalogue::byId)

    /** Limits and defaults for the setup answers. */
    public companion object {
        /** Fewest sessions a week a plan can be built for. */
        public const val MIN_SESSIONS_PER_WEEK: Int = 1

        /** Most sessions a week a plan can be built for. */
        public const val MAX_SESSIONS_PER_WEEK: Int = 7

        /** Sessions a week suggested at setup. */
        public const val DEFAULT_SESSIONS_PER_WEEK: Int = 3

        /** Shortest session a plan can be built for, in minutes. */
        public const val MIN_SESSION_MINUTES: Int = 10

        /** Longest session a plan can be built for, in minutes. */
        public const val MAX_SESSION_MINUTES: Int = 120

        /** Session length suggested at setup, in minutes. */
        public const val DEFAULT_SESSION_MINUTES: Int = 45
    }
}
