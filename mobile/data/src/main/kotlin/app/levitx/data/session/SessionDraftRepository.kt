package app.levitx.data.session

import app.levitx.core.session.SessionRun

/**
 * Keeps the session in progress safe between sets.
 *
 * A session runs for many minutes and the system can kill the app at any
 * point in it. Saving after every set means the worst case is losing nothing.
 */
interface SessionDraftRepository {
    /** The session in progress, or null when there isn't one (or it can no longer be read). */
    suspend fun load(): SessionRun?

    /** Saves [run] as the session in progress, replacing any earlier one. */
    suspend fun save(run: SessionRun)

    /** Forgets the session in progress, once it is finished or discarded. */
    suspend fun clear()
}
