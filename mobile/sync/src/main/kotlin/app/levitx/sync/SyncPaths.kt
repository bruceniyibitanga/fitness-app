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

    /** Finished workouts on their way to the phone's coach. Written by the watch; one item per workout. */
    const val WORKOUTS_PREFIX = "/levitx/workouts/"

    /**
     * The phone's confirmation that it has stored a session or workout.
     * Written by the phone; one item per id. Ids are random UUIDs, so the two
     * kinds can share one prefix.
     */
    const val ACKS_PREFIX = "/levitx/acks/"

    /** The data item path for the session with [sessionId]. */
    fun session(sessionId: String): String = SESSIONS_PREFIX + sessionId

    /** The data item path for the workout with [workoutId]. */
    fun workout(workoutId: String): String = WORKOUTS_PREFIX + workoutId

    /** The data item path confirming the session or workout with [id]. */
    fun ack(id: String): String = ACKS_PREFIX + id
}
