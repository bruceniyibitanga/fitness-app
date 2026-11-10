package app.levitx.testing

import app.levitx.core.session.CompletedSession
import app.levitx.data.session.SessionOutbox
import app.levitx.data.session.SessionRepository
import app.levitx.data.session.StoredSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory [SessionRepository] and [SessionOutbox]. Like the real one, saving
 * a session also raises the gate's best in [progress], and a session id is
 * only ever stored once.
 */
class FakeSessionRepository(private val progress: FakeProgressRepository) :
    SessionRepository,
    SessionOutbox {
    private val saved = mutableListOf<StoredSession>()
    private val queue = MutableStateFlow<List<StoredSession>>(emptyList())
    private var nextId = 0

    /** Sessions saved so far, oldest first. */
    val sessions: List<CompletedSession>
        get() = saved.map { it.session }

    /** Sessions saved so far with their ids, oldest first. */
    val stored: List<StoredSession>
        get() = saved.toList()

    /** Sessions still queued for the other device. */
    val queued: List<StoredSession>
        get() = queue.value

    override suspend fun saveCompletedSession(session: CompletedSession) {
        insertIfNew(StoredSession("local-${nextId++}", session))
    }

    override suspend fun saveReceivedSession(received: StoredSession): Boolean = insertIfNew(received)

    override suspend fun saveAndQueue(queued: StoredSession) {
        if (insertIfNew(queued)) queue.update { it + queued }
    }

    override fun observeQueued(): Flow<List<StoredSession>> = queue.asStateFlow()

    override suspend fun markDelivered(sessionId: String) {
        queue.update { sessions -> sessions.filterNot { it.id == sessionId } }
    }

    private suspend fun insertIfNew(stored: StoredSession): Boolean {
        if (saved.any { it.id == stored.id }) return false
        saved += stored
        if (stored.session.best > 0) progress.recordBest(stored.session.gateId, stored.session.best)
        return true
    }
}
