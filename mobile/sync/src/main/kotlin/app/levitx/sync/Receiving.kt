package app.levitx.sync

/*
 * How the phone takes in each kind of item the watch sends: workouts,
 * cardio and mobility workouts, and sessions from an older watch app.
 */

/** Payload of a confirmation item. Only its path matters; the byte is the wire version. */
private val ACK_PAYLOAD = byteArrayOf(WIRE_VERSION.toByte())

/**
 * Stores and confirms each item under [prefix]: [decode]s it, checks the
 * id inside ([idOf]) is the id in the path it arrived on, then [store]s
 * it, which returns true when it was new. Unreadable items are skipped
 * and left for a newer app. Confirming comes only after storing, and
 * confirming again is harmless.
 */
internal suspend fun <T> DataLayer.receiveEach(
    prefix: String,
    decode: (ByteArray) -> Decoded<T>,
    idOf: (T) -> String,
    store: suspend (T) -> Boolean,
): Received {
    val items = list(prefix)
    var stored = 0
    var skipped = 0
    for (item in items) {
        val decoded = decode(item.payload)
        if (decoded !is Decoded.Ok || item.path != prefix + idOf(decoded.value)) {
            skipped++
            continue
        }
        if (store(decoded.value)) stored++
        put(SyncPaths.ack(idOf(decoded.value)), ACK_PAYLOAD)
    }
    return Received(stored, skipped, items.map { it.path.removePrefix(prefix) }.toSet())
}

/**
 * What one kind of incoming item came to.
 *
 * @property stored Items newly recorded.
 * @property skipped Items left alone because they were unreadable.
 * @property waitingIds Ids of every item still on the Data Layer, recorded or not.
 */
internal data class Received(val stored: Int = 0, val skipped: Int = 0, val waitingIds: Set<String>)
