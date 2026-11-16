package app.levitx.testing

import app.levitx.core.history.WorkoutRecord
import app.levitx.data.history.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [HistoryRepository]. Set [workouts] to change what it emits. */
class FakeHistoryRepository(workouts: List<WorkoutRecord> = emptyList()) : HistoryRepository {
    /** The workouts it emits, newest first, as the real one does. */
    val workouts = MutableStateFlow(workouts.sortedByDescending { it.finishedAtEpochMillis })

    override fun observeWorkouts(): Flow<List<WorkoutRecord>> = workouts.asStateFlow()
}
