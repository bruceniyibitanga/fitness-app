package app.levitx.data.intervals

import app.levitx.core.session.CompletedSession
import kotlinx.coroutines.flow.Flow

/**
 * A cardio or mobility workout finished on the watch, on its way to the
 * phone's history.
 *
 * @property id The workout's id, a random UUID. The phone stores it under
 *   this id, so a delivery that is retried can't be saved twice.
 * @property sessions The work done, one session per move, as
 *   `IntervalRun.sessions` gives it. Never empty.
 */
data class QueuedIntervalWorkout(val id: String, val sessions: List<CompletedSession>) {
    init {
        require(sessions.isNotEmpty()) { "an interval workout with no work done isn't queued" }
    }
}

/**
 * Cardio and mobility workouts finished on the watch, waiting for the phone.
 *
 * The watch doesn't keep these once the phone has them: it has no history
 * screen, so it only holds them for as long as delivery takes. They never
 * reach the coach on either device.
 */
interface IntervalOutbox {
    /** Queues [workout]. Queuing the same id again changes nothing. */
    suspend fun queue(workout: QueuedIntervalWorkout)

    /** Workouts still waiting, oldest first. Emits straight away and whenever the queue changes. */
    fun observeQueued(): Flow<List<QueuedIntervalWorkout>>

    /** Deletes a workout from the watch once the phone has it. */
    suspend fun markDelivered(workoutId: String)
}
