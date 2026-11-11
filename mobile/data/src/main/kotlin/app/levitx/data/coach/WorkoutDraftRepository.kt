package app.levitx.data.coach

import app.levitx.core.coach.WorkoutRun
import kotlinx.coroutines.flow.Flow

/**
 * The workout in progress, saved after every change so it survives the app
 * being killed. There is at most one. It is cleared when the workout is
 * recorded through [CoachRepository.recordWorkout], or abandoned.
 */
interface WorkoutDraftRepository {
    /** Emits the workout in progress straight away and whenever it changes. Null when there is none. */
    fun observeDraft(): Flow<WorkoutRun?>

    /** Saves [run] as the workout in progress, replacing any other. */
    suspend fun save(run: WorkoutRun)

    /** Throws the workout in progress away without recording it. */
    suspend fun discard()
}
