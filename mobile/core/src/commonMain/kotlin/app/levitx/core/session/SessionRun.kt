package app.levitx.core.session

private const val MILLIS_PER_SECOND = 1_000L

/**
 * The vest, belt or band a set is done with.
 *
 * @property addedKg Weight carried. 0 for none.
 * @property assistanceKg Help from a band, as calibrated. 0 for none.
 * @throws IllegalArgumentException if a figure is negative or above the load limits.
 */
public data class SetLoad(val addedKg: Double = 0.0, val assistanceKg: Double = 0.0) {
    init {
        require(addedKg.isValidModifier()) { "addedKg out of range" }
        require(assistanceKg.isValidModifier()) { "assistanceKg out of range" }
    }

    public companion object {
        /** Bodyweight only. */
        public val None: SetLoad = SetLoad()
    }
}

/**
 * A session in progress: the plan, what has been done so far and whether the
 * person is resting.
 *
 * It is immutable. Each action returns the next state, so the rules for moving
 * through a session live here once, and both apps (and their tests) share them.
 *
 * @property id Unique id the finished session is stored and synced under. Made
 *   when the session starts, so saving it twice can never create two sessions.
 * @property plan Today's plan. Fixed when the session starts, so targets don't
 *   move mid-session.
 * @property previousBest The person's best at the gate before this session.
 * @property startedAtEpochMillis When the session started, in UTC milliseconds.
 * @property logged Sets done so far, in order.
 * @property restEndsAtElapsedMillis When rest before the next set ends, on the
 *   device's elapsed (since boot) clock. Null when not resting.
 * @property load The load the next set will be logged with.
 * @throws IllegalArgumentException if [id] is blank, [previousBest] is negative,
 *   or more sets are logged than the plan has.
 */
public data class SessionRun(
    val id: String,
    val plan: SessionPlan,
    val previousBest: Int,
    val startedAtEpochMillis: Long,
    val logged: List<LoggedSet> = emptyList(),
    val restEndsAtElapsedMillis: Long? = null,
    val load: SetLoad = SetLoad.None,
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(previousBest >= 0) { "previousBest must not be negative" }
        require(logged.size <= plan.sets.size) { "more sets logged than planned" }
    }

    /** True once every planned set is logged. */
    val isComplete: Boolean
        get() = logged.size >= plan.sets.size

    /** True while the rest timer is running. */
    val isResting: Boolean
        get() = restEndsAtElapsedMillis != null

    /** The set to do next, or null once the session is complete. */
    val nextSet: PlannedSet?
        get() = plan.sets.getOrNull(logged.size)

    /** 0-based index of the set to do next. Stays on the last set once complete. */
    val nextSetIndex: Int
        get() = logged.size.coerceAtMost(plan.sets.lastIndex)

    /** Changes the load the next set will be logged with. */
    public fun withLoad(load: SetLoad): SessionRun = copy(load = load)

    /**
     * Records the set just done with the current [load], and starts rest if
     * more sets remain. Does nothing during rest or once the session is complete.
     *
     * @param value Reps or seconds done. Negative values count as 0.
     * @param repsInReserve Reps the person felt they had left, or null if they didn't say.
     * @param nowElapsedMillis The elapsed clock now, which rest is timed from.
     */
    public fun logSet(value: Int, repsInReserve: Int?, nowElapsedMillis: Long): SessionRun {
        val planned = nextSet
        if (planned == null || isResting) return this
        val sets = logged + LoggedSet(
            value = value.coerceAtLeast(0),
            repsInReserve = repsInReserve?.coerceAtLeast(0),
            isBestEffort = planned.isBestEffort,
            addedKg = load.addedKg,
            assistanceKg = load.assistanceKg,
        )
        val restEndsAt = if (sets.size < plan.sets.size) {
            nowElapsedMillis + plan.restSeconds * MILLIS_PER_SECOND
        } else {
            null
        }
        return copy(logged = sets, restEndsAtElapsedMillis = restEndsAt)
    }

    /** Ends rest, early or because it ran out. */
    public fun endRest(): SessionRun = copy(restEndsAtElapsedMillis = null)

    /**
     * The session as it stands, ready to save, or null when no set was done.
     *
     * @param finishedAtEpochMillis Now, in UTC milliseconds. If the device clock
     *   was moved back mid-session, the start time is used instead.
     * @param bodyweightKg Bodyweight to save with the session, if known.
     */
    public fun complete(finishedAtEpochMillis: Long, bodyweightKg: Double? = null): CompletedSession? {
        if (logged.isEmpty()) return null
        return CompletedSession(
            gateId = plan.gate.id,
            startedAtEpochMillis = startedAtEpochMillis,
            finishedAtEpochMillis = maxOf(finishedAtEpochMillis, startedAtEpochMillis),
            sets = logged,
            bodyweightKg = bodyweightKg,
        )
    }
}
