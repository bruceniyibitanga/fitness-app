package app.levitx.data.history

import app.levitx.core.history.WorkoutRecord
import kotlinx.coroutines.flow.Flow

/** Finished workouts, for the progress and history screens. Read only. */
interface HistoryRepository {
    /** Every finished workout, newest first. Emits again when one is stored. */
    fun observeWorkouts(): Flow<List<WorkoutRecord>>
}
