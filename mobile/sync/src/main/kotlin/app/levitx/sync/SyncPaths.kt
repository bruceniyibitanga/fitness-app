package app.levitx.sync

/**
 * Where each kind of data item lives on the Data Layer.
 *
 * The listener services in each app's manifest filter on these same paths, so
 * a change here must be made there too.
 */
internal object SyncPaths {
    /** What the watch needs to run a session. Written by the phone; one item. */
    const val STATE = "/levitx/state"

    /** Finished sessions on their way to the phone. Written by the watch; one item per session. */
    const val SESSIONS_PREFIX = "/levitx/sessions/"

    /** The phone's confirmation that it has stored a session. Written by the phone; one item per session. */
    const val ACKS_PREFIX = "/levitx/acks/"

    /** The data item path for the session with [sessionId]. */
    fun session(sessionId: String): String = SESSIONS_PREFIX + sessionId

    /** The data item path confirming the session with [sessionId]. */
    fun ack(sessionId: String): String = ACKS_PREFIX + sessionId
}
