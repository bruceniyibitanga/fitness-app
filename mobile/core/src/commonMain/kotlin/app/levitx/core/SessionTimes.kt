package app.levitx.core

/**
 * A start and finish time moved back together so the finish is no later than
 * [limitEpochMillis], keeping the time between them. Unchanged when it
 * already is. Finished sessions use it so none is ever dated in the future.
 */
internal fun endingBy(
    startedAtEpochMillis: Long,
    finishedAtEpochMillis: Long,
    limitEpochMillis: Long,
): Pair<Long, Long> {
    val ahead = (finishedAtEpochMillis - limitEpochMillis).coerceAtLeast(0)
    return (startedAtEpochMillis - ahead) to (finishedAtEpochMillis - ahead)
}
