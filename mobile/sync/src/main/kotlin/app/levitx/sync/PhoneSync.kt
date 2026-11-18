package app.levitx.sync

import app.levitx.data.ApplicationScope
import app.levitx.data.DataUnavailableException
import app.levitx.data.privacy.ResetMarker
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
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
    private val inbox: PhoneInbox,
    private val resetMarker: ResetMarker,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    /**
     * Starts publishing the goal, bests, bands and next session whenever they
     * change, and picks up any workouts that arrived while the app wasn't
     * running. Call once, when the app process starts.
     */
    fun start() {
        scope.launch {
            // Before anything new is published: a delete the watch may not have heard about goes first.
            resetMarker.current()?.let { engine.announceReset(it).log("phone reset") }
            engine.watchState()
                // Locked data is expected and already shown on screen. Anything else is a bug.
                .catch { error -> if (error !is DataUnavailableException) throw error }
                .collect { state ->
                    if (state == null) {
                        // Nothing to offer: before setup, or after everything was deleted.
                        engine.withdraw(resetMarker.current()).log("phone withdraw")
                    } else {
                        engine.publish(state).log("phone publish")
                    }
                }
        }
        scope.launch { receive() }
    }

    /**
     * Records and confirms workouts the watch has sent. Call when the system
     * reports that workout or session data items changed. Returns when the
     * pass is done.
     */
    suspend fun receive() {
        inbox.receive().log("phone receive")
        // A new workout changes the next session and can raise a best, which the watch should hear about now.
        engine.publishCurrent().log("phone publish")
    }

    /**
     * Stops offering the watch anything about the person and tells it to
     * delete its copy, when they delete everything. Returns when done.
     *
     * The delete gets a new random id, saved before anything is sent. If the
     * watch can't be told now (no link, or the app is closed first), the id
     * is offered again every time the app starts, and the watch deletes its
     * copy when it first sees an id it hasn't acted on.
     */
    suspend fun forget() {
        val resetId = UUID.randomUUID().toString()
        resetMarker.set(resetId)
        engine.forget(resetId).log("phone forget")
    }

    /**
     * [receive] for a `WearableListenerService` callback.
     *
     * The system may stop a listener service as soon as its callback returns,
     * so the pass has to finish inside it. The callback already runs on a
     * background thread, which is why blocking it is fine here (and only here).
     * The time limit keeps a stuck pass from holding the service open.
     */
    fun receiveBlocking() {
        runBlocking { withTimeoutOrNull(LISTENER_TIMEOUT_MILLIS) { receive() } }
    }
}
