package app.levitx.sync

/**
 * One data item on the Data Layer.
 *
 * @property path Its path, such as `/levitx/state`.
 * @property payload Its bytes. Empty when the item has none.
 * @property isOwn True when this device created the item. Only its creator may delete it.
 */
internal class DataLayerItem(val path: String, val payload: ByteArray, val isOwn: Boolean)

/**
 * The few Data Layer operations sync needs.
 *
 * It exists so the sync rules can be tested against an in-memory fake. The
 * real implementation, [PlayServicesDataLayer], is a thin wrapper around
 * Google's `DataClient` and nothing else talks to that API.
 *
 * Data items are stored by Google Play services and delivered to the person's
 * other devices whenever they are next connected, so every call here works
 * offline. Every function throws [DataLayerUnavailableException] when the
 * Data Layer can't be used on this device.
 */
internal interface DataLayer {
    /**
     * Creates this device's item at [path], or replaces its payload. Putting
     * the same payload again changes nothing and notifies no one.
     */
    suspend fun put(path: String, payload: ByteArray)

    /** Deletes this device's own item at [path]. Does nothing if there isn't one. */
    suspend fun delete(path: String)

    /** Items from any of the person's devices whose path starts with [pathPrefix]. */
    suspend fun list(pathPrefix: String): List<DataLayerItem>
}

/**
 * The Data Layer can't be used right now: Google Play services is missing or
 * out of date, or the call failed. It is expected (plenty of phones have no
 * watch), so sync skips the pass and tries again next time.
 */
internal class DataLayerUnavailableException(cause: Throwable) : Exception("The Data Layer is unavailable", cause)
