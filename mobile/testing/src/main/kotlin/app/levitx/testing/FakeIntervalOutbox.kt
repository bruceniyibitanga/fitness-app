package app.levitx.testing

import app.levitx.data.intervals.IntervalOutbox
import app.levitx.data.intervals.QueuedIntervalWorkout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [IntervalOutbox]. Like the real one, queuing an id twice keeps the first. */
class FakeIntervalOutbox : IntervalOutbox {
    private val queued = MutableStateFlow<List<QueuedIntervalWorkout>>(emptyList())

    /** Workouts still queued, for assertions. */
    val current: List<QueuedIntervalWorkout>
        get() = queued.value

    override suspend fun queue(workout: QueuedIntervalWorkout) {
        queued.update { workouts -> if (workouts.any { it.id == workout.id }) workouts else workouts + workout }
    }

    override fun observeQueued(): Flow<List<QueuedIntervalWorkout>> = queued.asStateFlow()

    override suspend fun markDelivered(workoutId: String) {
        queued.update { workouts -> workouts.filterNot { it.id == workoutId } }
    }
}
