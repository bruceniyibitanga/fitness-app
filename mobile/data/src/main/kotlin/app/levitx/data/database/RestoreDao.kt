package app.levitx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Writes a restore: clears the person's data, then puts a backup's in its
 * place. The caller runs both in one transaction, so a restore that is cut
 * short leaves the phone as it was. Short-lived rows (the workout in
 * progress and the queues between phone and watch) are left alone.
 */
@Dao
internal interface RestoreDao {
    /** True while a workout is in progress: a restore would pull it out from under the person. */
    @Query("SELECT EXISTS(SELECT 1 FROM workout_draft) OR EXISTS(SELECT 1 FROM session_draft)")
    suspend fun hasWorkoutInProgress(): Boolean

    // Sets go with their sessions: logged_set's foreign key cascades.
    @Query("DELETE FROM training_session")
    suspend fun clearSessions()

    @Query("DELETE FROM gate_best")
    suspend fun clearGateBests()

    @Query("DELETE FROM band")
    suspend fun clearBands()

    @Query("DELETE FROM bodyweight_entry")
    suspend fun clearWeighIns()

    @Query("DELETE FROM active_goal")
    suspend fun clearGoal()

    @Query("DELETE FROM coach_state")
    suspend fun clearCoach()

    // Worked out from the coach's state, so it's rebuilt from the restored one.
    @Query("DELETE FROM next_session")
    suspend fun clearNextSession()

    /** Inserts a backup's plan: the coach's state and the goal. */
    @Insert
    suspend fun insertPlan(coach: List<CoachStateEntity>, goal: List<ActiveGoalEntity>)

    /** Inserts a backup's history, sessions before their sets. */
    @Insert
    suspend fun insertHistory(
        weighIns: List<BodyweightEntity>,
        bands: List<BandEntity>,
        bests: List<GateBestEntity>,
        sessions: List<TrainingSessionEntity>,
        sets: List<LoggedSetEntity>,
    )
}
