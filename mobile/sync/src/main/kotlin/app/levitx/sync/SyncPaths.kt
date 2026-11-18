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

    /**
     * Put there by the phone when the person deletes everything, and taken
     * away when the phone has a plan again. While it is there, the watch
     * deletes its own copy too. Written by the phone; one item, no payload.
     */
    const val FORGET = "/levitx/forget"

    /** Finished sessions on their way to the phone. Written by the watch; one item per session. */
    const val SESSIONS_PREFIX = "/levitx/sessions/"

    /** Finished workouts on their way to the phone's coach. Written by the watch; one item per workout. */
    const val WORKOUTS_PREFIX = "/levitx/workouts/"

    /**
     * Finished cardio and mobility workouts on their way to the phone's
     * history. Written by the watch; one item per workout.
     */
    const val INTERVALS_PREFIX = "/levitx/intervals/"

    /**
     * The phone's confirmation that it has stored a session or workout of
     * any kind. Written by the phone; one item per id. Ids are random UUIDs,
     * so every kind can share one prefix.
     */
    const val ACKS_PREFIX = "/levitx/acks/"

    /** The data item path for the session with [sessionId]. */
    fun session(sessionId: String): String = SESSIONS_PREFIX + sessionId

    /** The data item path for the workout with [workoutId]. */
    fun workout(workoutId: String): String = WORKOUTS_PREFIX + workoutId

    /** The data item path for the cardio or mobility workout with [workoutId]. */
    fun intervals(workoutId: String): String = INTERVALS_PREFIX + workoutId

    /** The data item path confirming the session or workout with [id]. */
    fun ack(id: String): String = ACKS_PREFIX + id
}
