package app.levitx.core.coach

/**
 * When to ask for a quick weigh-in before a session (COACH-ENGINE.md section 6).
 *
 * Only when there's no reading from the last week, and at most once a week,
 * so someone who would rather not weigh in is asked rarely and never nagged.
 */
public object WeighInPrompt {
    /** Days a reading stays fresh, and the shortest time between two asks. */
    public const val INTERVAL_DAYS: Int = 7

    private const val DAY_MILLIS = 24L * 60 * 60 * 1_000
    private const val INTERVAL_MILLIS = INTERVAL_DAYS * DAY_MILLIS

    /**
     * True when the prompt should show now.
     *
     * @param weighIns Recorded readings, in any order.
     * @param lastAskedAtEpochMillis When the prompt last showed, saved or skipped, or null if never.
     * @param nowEpochMillis The current time.
     */
    public fun isDue(weighIns: List<WeighIn>, lastAskedAtEpochMillis: Long?, nowEpochMillis: Long): Boolean {
        val fresh = weighIns.any { nowEpochMillis - it.atEpochMillis in 0 until INTERVAL_MILLIS }
        // A time in the future means the clock went back; ask rather than wait an unknown time.
        val askedLately = lastAskedAtEpochMillis != null &&
            nowEpochMillis - lastAskedAtEpochMillis in 0 until INTERVAL_MILLIS
        return !fresh && !askedLately
    }
}
