package app.levitx.sync

import app.levitx.data.ApplicationScope
import app.levitx.data.DataUnavailableException
import app.levitx.data.session.SessionOutbox
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Keeps the watch in step with the phone. This is the only sync class the
 * watch app uses.
 */
@Singleton
class WatchSync @Inject internal constructor(
    private val engine: WatchSyncEngine,
    private val outbox: SessionOutbox,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    /**
     * Syncs now, and again whenever a session is queued. Call once, when the
     * app process starts.
     */
    fun start() {
        scope.launch {
            outbox.observeQueued()
                // If sessions are queued faster than passes run, one pass covers them all.
                .conflate()
                // Locked data is expected and already shown on screen. Anything else is a bug.
                .catch { error -> if (error !is DataUnavailableException) throw error }
                .collect { syncNow() }
        }
    }

    /**
     * Runs one sync pass. Call when the system reports that the phone's data
     * items changed. Returns when the pass is done.
     */
    suspend fun syncNow() {
        engine.sync().log("watch sync")
    }

    /**
     * [syncNow] for a `WearableListenerService` callback.
     *
     * The system may stop a listener service as soon as its callback returns,
     * so the pass has to finish inside it. The callback already runs on a
     * background thread, which is why blocking it is fine here (and only here).
     * The time limit keeps a stuck pass from holding the service open.
     */
    fun syncNowBlocking() {
        runBlocking { withTimeoutOrNull(LISTENER_TIMEOUT_MILLIS) { syncNow() } }
    }
}
