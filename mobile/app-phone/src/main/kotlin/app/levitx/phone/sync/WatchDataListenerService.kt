package app.levitx.phone.sync

import app.levitx.sync.PhoneSync
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Wakes the phone app when the watch has a finished workout for it.
 *
 * Google Play services starts this service even when the app isn't running,
 * which is why it is a manifest-registered listener and not a live one. The
 * manifest filter limits it to this app's own workout and session paths.
 */
@AndroidEntryPoint
class WatchDataListenerService : WearableListenerService() {
    /** Records and confirms the workouts. Set by Hilt. */
    @Inject
    lateinit var phoneSync: PhoneSync

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // The events only say that something changed. The pass reads the
        // current items itself, so a missed or repeated event does no harm.
        phoneSync.receiveBlocking()
    }
}
