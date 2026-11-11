package app.levitx.data.coach

import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.WorkoutRun
import kotlinx.coroutines.flow.Flow

/**
 * The next session the phone's coach planned, as the watch keeps it.
 *
 * The coach runs on the phone, where the person's model is. The watch only
 * gets the plan it produced, so it can run that session with the phone out of
 * reach without holding any health data.
 */
interface NextSessionRepository {
    /** The next session, or null when there isn't one. Emits straight away and on every change. */
    fun observeNext(): Flow<PlannedSession?>

    /**
     * A workout finished on the watch that the phone hasn't recorded yet, or
     * null. While there is one, the phone's plan predates it and isn't taken.
     */
    suspend fun awaitingWorkoutId(): String?

    /** Saves [session] from the phone as the next one, or null for none, and stops waiting. */
    suspend fun replace(session: PlannedSession?)
}

/**
 * A workout finished on this device, waiting for the other device to confirm it.
 *
 * @property run The workout as it finished.
 * @property finishedAtEpochMillis When it finished, in UTC milliseconds.
 */
data class QueuedWorkout(val run: WorkoutRun, val finishedAtEpochMillis: Long)

/**
 * Workouts finished on the watch, on their way to the phone's coach.
 *
 * Finishing is one transaction: the workout is queued, the watch's own copy
 * of the gate bests is raised, the workout in progress is cleared, and the
 * next session is cleared until the phone plans a new one. A crash can't leave a workout both finished and
 * still in progress, or queued twice.
 */
interface WorkoutOutbox {
    /**
     * Queues [run] as finished at [finishedAtEpochMillis], all in one go as
     * described above, and has the next session wait for the phone to record
     * it. Safe to call again with the same workout: nothing changes. A run with
     * no sets done isn't queued; the workout in progress is simply cleared and
     * the next session is kept.
     *
     * @return The gates whose best this raised, with the new best. Empty when
     *   nothing was raised, including when the workout was already queued.
     */
    suspend fun finishAndQueue(run: WorkoutRun, finishedAtEpochMillis: Long): Map<String, Int>

    /** Workouts still waiting, oldest first. Emits straight away and whenever the queue changes. */
    fun observeQueued(): Flow<List<QueuedWorkout>>

    /** Takes a workout off the queue once the phone has it. */
    suspend fun markDelivered(workoutId: String)
}
