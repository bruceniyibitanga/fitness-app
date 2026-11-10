package app.levitx.data.session

import app.levitx.core.session.CompletedSession

/** Saves finished training sessions. */
interface SessionRepository {
    /**
     * Saves [session] and its sets, and raises the gate's best if the session
     * beat it. All of it happens in one transaction: either everything is
     * saved or nothing is.
     */
    suspend fun saveCompletedSession(session: CompletedSession)
}
