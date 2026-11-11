package app.levitx.core.coach

import app.levitx.core.session.LoggedSet

/**
 * How a set felt, as the one-tap rating the apps ask for.
 *
 * Each maps to reps in reserve, the measure the rest of the core uses, so a
 * rating and a typed number are the same data (Zourdos et al. 2016).
 *
 * @property repsInReserve The reps in reserve this rating is stored as.
 */
public enum class Effort(public val repsInReserve: Int) {
    /** Four or more reps left. */
    EASY(repsInReserve = 4),

    /** Two or three left. What work sets aim for. */
    RIGHT(repsInReserve = 2),

    /** One or none left. */
    HARD(repsInReserve = 0),
    ;

    /** Reading a rating back from stored reps in reserve. */
    public companion object {
        /** The rating for [repsInReserve], or null when the person didn't say. */
        public fun of(repsInReserve: Int?): Effort? = when {
            repsInReserve == null -> null
            repsInReserve >= EASY.repsInReserve -> EASY
            repsInReserve >= RIGHT.repsInReserve -> RIGHT
            else -> HARD
        }
    }
}

/** What the person chose when the coach suggested easing off. */
public enum class SuggestionChoice {
    /** Do the session as planned. */
    KEEP,

    /** Do the lighter version. */
    LIGHTER,

    /** Rest today. The session waits. */
    REST,
}

/**
 * What happened to one exercise of a session.
 *
 * @property plan The exercise as planned, or as picked from the Train tab.
 * @property sets The sets done, in order. Empty when skipped.
 * @property skipped True when the person skipped or swapped the exercise out.
 */
public data class ExerciseResult(val plan: ExercisePlan, val sets: List<LoggedSet>, val skipped: Boolean = false) {
    /** The best set that counts toward a skill gate: unassisted. 0 when there is none. */
    val gateTop: Int
        get() = sets.filter { it.countsTowardGate }.maxOfOrNull { it.value } ?: 0
}

/**
 * A finished session, as the coach learns from it. Sessions started from the
 * Train tab are reported the same way, so off-plan work still counts.
 *
 * @property results One entry per exercise, in the order done.
 * @property startedAtEpochMillis When the session started, in UTC milliseconds.
 * @property finishedAtEpochMillis When it finished, in UTC milliseconds.
 * @property choice What the person picked if the coach had suggested easing off, else null.
 * @throws IllegalArgumentException if the session finishes before it starts.
 */
public data class SessionResult(
    val results: List<ExerciseResult>,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long,
    val choice: SuggestionChoice? = null,
) {
    init {
        require(finishedAtEpochMillis >= startedAtEpochMillis) { "a session can't finish before it starts" }
    }

    /**
     * The best strict set per skill gate trained hard in this session, the
     * same figure the coach raises a gate's best to. Gates with no strict set
     * are left out. Light work doesn't count, as it doesn't for the coach.
     */
    val gateBests: Map<String, Int>
        get() = results
            .filter { it.plan.isHard && it.gateTop > 0 }
            .mapNotNull { result -> result.plan.gateId?.let { it to result.gateTop } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, tops) -> tops.max() }

    /**
     * The gates this session set a new best on, with the new best, judged
     * against the bests from [before] it.
     */
    public fun newGateBests(before: Map<String, Int>): Map<String, Int> =
        gateBests.filter { (gateId, best) -> best > (before[gateId] ?: 0) }
}
