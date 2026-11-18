package app.levitx.sync

import app.levitx.data.privacy.PrivacyRepository
import app.levitx.data.privacy.ResetMarker
import javax.inject.Inject

/**
 * Deletes everything the watch holds when the phone says the person deleted
 * everything there.
 *
 * The phone says so in two ways. [SyncPaths.FORGET] is there from the delete
 * until the phone has a plan again. [SyncPaths.RESET] holds the id of the
 * last delete for good: a watch that was out of touch for all of that still
 * finds an id it hasn't acted on, and deletes then.
 */
internal class WatchWipe @Inject constructor(
    private val privacy: PrivacyRepository,
    private val resetMarker: ResetMarker,
) {
    /** Deletes every row in the watch's database. Settings such as the unit are kept. */
    suspend fun wipe() {
        privacy.deleteEverything()
    }

    /**
     * Deletes everything if [resetId] is a delete this watch hasn't acted
     * on, then remembers it. The id is saved only after the delete, so a
     * delete that is cut short is done again on the next pass.
     *
     * @return True when it deleted.
     */
    suspend fun applyReset(resetId: String): Boolean {
        if (resetMarker.current() == resetId) return false
        privacy.deleteEverything()
        resetMarker.set(resetId)
        return true
    }
}
