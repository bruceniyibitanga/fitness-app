package app.levitx.wear.sync

import app.levitx.sync.WatchSync
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Wakes the watch app when the phone changes something the watch cares about:
 * the goal, bests and bands, or a confirmation that a session arrived.
 *
 * Google Play services starts this service even when the app isn't running,
 * which is why it is a manifest-registered listener and not a live one. The
 * manifest filter limits it to this app's own sync paths.
 */
@AndroidEntryPoint
class PhoneDataListenerService : WearableListenerService() {
    /** Runs the sync pass. Set by Hilt. */
    @Inject
    lateinit var watchSync: WatchSync

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // The events only say that something changed. The pass reads the
        // current items itself, so a missed or repeated event does no harm.
        watchSync.syncNowBlocking()
    }
}
