package app.levitx.sync

import app.levitx.data.ApplicationScope
import app.levitx.data.DataUnavailableException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Keeps the watch in step with the phone. This is the only sync class the
 * phone app uses.
 *
 * Nothing here is sent to a server we run. Data items travel between the
 * person's own devices through Google Play services (see docs/app/SECURITY.md).
 */
@Singleton
class PhoneSync @Inject internal constructor(
    private val engine: PhoneSyncEngine,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    /**
     * Starts publishing the goal, bests and bands whenever they change, and
     * picks up any sessions that arrived while the app wasn't running. Call
     * once, when the app process starts.
     */
    fun start() {
        scope.launch {
            engine.watchState()
                .filterNotNull()
                // Locked data is expected and already shown on screen. Anything else is a bug.
                .catch { error -> if (error !is DataUnavailableException) throw error }
                .collect { state -> engine.publish(state).log("phone publish") }
        }
        scope.launch { receiveSessions() }
    }

    /**
     * Stores and confirms sessions the watch has sent. Call when the system
     * reports that session data items changed. Returns when the pass is done.
     */
    suspend fun receiveSessions() {
        engine.receiveSessions().log("phone receive")
        // A new session can raise a best, which the watch should hear about before this returns.
        engine.publishCurrent().log("phone publish")
    }

    /**
     * [receiveSessions] for a `WearableListenerService` callback.
     *
     * The system may stop a listener service as soon as its callback returns,
     * so the pass has to finish inside it. The callback already runs on a
     * background thread, which is why blocking it is fine here (and only here).
     * The time limit keeps a stuck pass from holding the service open.
     */
    fun receiveSessionsBlocking() {
        runBlocking { withTimeoutOrNull(LISTENER_TIMEOUT_MILLIS) { receiveSessions() } }
    }
}
