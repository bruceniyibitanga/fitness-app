package app.levitx.testing

import app.levitx.core.session.CompletedSession
import app.levitx.data.session.SessionRepository

/**
 * In-memory [SessionRepository]. Like the real one, saving a session also
 * raises the gate's best in [progress].
 */
class FakeSessionRepository(private val progress: FakeProgressRepository) : SessionRepository {
    private val saved = mutableListOf<CompletedSession>()

    /** Sessions saved so far, oldest first. */
    val sessions: List<CompletedSession>
        get() = saved.toList()

    override suspend fun saveCompletedSession(session: CompletedSession) {
        saved += session
        progress.recordBest(session.gateId, session.best)
    }
}
