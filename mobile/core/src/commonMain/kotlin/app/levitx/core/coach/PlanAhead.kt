package app.levitx.core.coach

/**
 * The coach's plan for a device that can't ask it: the watch, with the phone
 * out of reach.
 *
 * What the coach recommends depends on when it is asked. Straight after a
 * session, everything just trained needs rest, so the session for "now" is
 * mostly light work. A day and a half later the same question gets the real
 * next session. A plan worked out once and done days later would be the
 * light one, so the phone sends both: the session for now, and the session
 * for once the rest is over, with the time it takes over.
 *
 * Starting before that time gets [now], which is always safe then. Starting
 * after it gets [rested].
 *
 * @property now The session to do before [restedAtEpochMillis], or at any time when there is no [rested].
 * @property rested The session to do from [restedAtEpochMillis] on, or null when it would be the same as [now].
 * @property restedAtEpochMillis When [rested] takes over, in UTC milliseconds. Null exactly when [rested] is.
 * @throws IllegalArgumentException if only one of [rested] and [restedAtEpochMillis] is given.
 */
public data class PlanAhead(
    val now: PlannedSession,
    val rested: PlannedSession? = null,
    val restedAtEpochMillis: Long? = null,
) {
    init {
        require((rested == null) == (restedAtEpochMillis == null)) { "rested and its time go together" }
    }

    /** The session to do when starting at [epochMillis]. */
    public fun at(epochMillis: Long): PlannedSession =
        if (rested != null && restedAtEpochMillis != null && epochMillis >= restedAtEpochMillis) rested else now
}
