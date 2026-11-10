package app.levitx.core.session

/**
 * One set as the person did it.
 *
 * @property value Reps or seconds done.
 * @property repsInReserve How many more reps they think they had left, or null
 *   for holds and when they didn't say.
 * @property isBestEffort True for the session's best-effort set.
 * @throws IllegalArgumentException if [value] or [repsInReserve] is negative.
 */
public data class LoggedSet(val value: Int, val repsInReserve: Int?, val isBestEffort: Boolean) {
    init {
        require(value >= 0) { "value must not be negative" }
        require(repsInReserve == null || repsInReserve >= 0) { "repsInReserve must not be negative" }
    }
}

/**
 * A finished session, ready to save.
 *
 * @property gateId The gate the session trained.
 * @property startedAtEpochMillis When the first set started, in UTC milliseconds.
 * @property finishedAtEpochMillis When the session was finished, in UTC milliseconds.
 * @property sets The sets in the order they were done. At least one.
 */
public data class CompletedSession(
    val gateId: String,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long,
    val sets: List<LoggedSet>,
) {
    init {
        require(sets.isNotEmpty()) { "a session needs at least one set" }
        require(finishedAtEpochMillis >= startedAtEpochMillis) { "a session can't finish before it starts" }
    }

    /** The best set of the session, which becomes the gate's best if it beats the old one. */
    val best: Int
        get() = sets.maxOf { it.value }
}
