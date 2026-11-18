package app.levitx.data.privacy

/**
 * The id of the last "delete everything", so the delete reaches the watch
 * even when the two were out of touch at the time.
 *
 * The phone makes a new random id each time the person deletes everything and
 * keeps offering it to the watch. The watch remembers the last id it acted
 * on, and when the phone's is a different one it deletes its own copy first.
 * So it doesn't matter whether the watch was in reach when the person
 * deleted, or whether they have set up again since.
 *
 * The id is random and says nothing about the person, which is why it can
 * live outside the encrypted database and survive the delete itself.
 */
interface ResetMarker {
    /**
     * On the phone, the id of the last delete here. On the watch, the last
     * id it acted on. Null when there has never been one.
     */
    suspend fun current(): String?

    /** Remembers [resetId] as the current one. */
    suspend fun set(resetId: String)
}
