package app.levitx.sync

/**
 * In-memory [DataLayer] shared by a pretend phone and a pretend watch.
 *
 * [onDevice] gives each device its own view: the same items, but each device
 * only owns, replaces and deletes what it put there itself, as on the real
 * Data Layer.
 */
class FakeNetwork {
    private class Entry(val owner: String, val payload: ByteArray)

    private val items = mutableMapOf<String, Entry>()

    /** Set to make every call fail, as when Play services is missing. */
    var isAvailable = true

    /** Paths of every item, sorted, for assertions. */
    val paths: List<String>
        get() = items.keys.sorted()

    /** Puts an item on the network as [owner], for tests that need a hand-made payload. */
    fun plant(owner: String, path: String, payload: ByteArray) {
        items[path] = Entry(owner, payload)
    }

    internal fun onDevice(device: String): DataLayer = object : DataLayer {
        override suspend fun put(path: String, payload: ByteArray) {
            checkAvailable()
            items[path] = Entry(device, payload)
        }

        override suspend fun delete(path: String) {
            checkAvailable()
            if (items[path]?.owner == device) items.remove(path)
        }

        override suspend fun list(pathPrefix: String): List<DataLayerItem> {
            checkAvailable()
            return items.filterKeys { it.startsWith(pathPrefix) }
                .map { (path, entry) -> DataLayerItem(path, entry.payload, isOwn = entry.owner == device) }
        }
    }

    private fun checkAvailable() {
        if (!isAvailable) throw DataLayerUnavailableException(IllegalStateException("unavailable"))
    }
}
