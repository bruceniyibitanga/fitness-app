package app.levitx.data.session

import app.levitx.core.session.CompletedSession
import kotlinx.coroutines.flow.Flow

/**
 * A finished session together with the id it is stored under.
 *
 * The id is made on the device that recorded the session and travels with it,
 * so the phone and the watch always mean the same session by it.
 *
 * @property id Unique id (a random UUID).
 * @property session The session.
 */
data class StoredSession(val id: String, val session: CompletedSession)

/** Saves finished training sessions. */
interface SessionRepository {
    /**
     * Saves [session] and its sets, and raises the gate's best if the session
     * beat it. All of it happens in one transaction: either everything is
     * saved or nothing is.
     */
    suspend fun saveCompletedSession(session: CompletedSession)

    /**
     * Saves a session that was recorded on the other device, the same way as
     * [saveCompletedSession]. Safe to call again with a session already
     * stored: nothing changes, so a delivery that is retried can't double-count.
     *
     * @return True when the session was new, false when it was already stored.
     */
    suspend fun saveReceivedSession(received: StoredSession): Boolean
}

/**
 * Sessions recorded on this device that the other device hasn't confirmed yet.
 *
 * The watch saves through this, so a session is stored and queued in one
 * step. Whatever sends them reads [observeQueued] and calls [markDelivered]
 * once the other side has confirmed.
 */
interface SessionOutbox {
    /**
     * Saves [queued] like [SessionRepository.saveCompletedSession] and queues
     * it for the other device, in one transaction. Safe to call again with
     * the same id: nothing changes.
     */
    suspend fun saveAndQueue(queued: StoredSession)

    /** Sessions still waiting, oldest first. Emits straight away and whenever the queue changes. */
    fun observeQueued(): Flow<List<StoredSession>>

    /** Takes a session off the queue once the other device has it. The session itself is kept. */
    suspend fun markDelivered(sessionId: String)
}
