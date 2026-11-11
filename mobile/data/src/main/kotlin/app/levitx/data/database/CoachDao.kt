package app.levitx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Reads and writes the coach's state and the workout in progress. */
@Dao
internal interface CoachDao {
    /** The coach's state, or null before setup. Emits again when it changes. */
    @Query("SELECT * FROM coach_state WHERE id = $COACH_ROW_ID")
    fun observeState(): Flow<CoachStateEntity?>

    /** The coach's state now, or null before setup. */
    @Query("SELECT * FROM coach_state WHERE id = $COACH_ROW_ID")
    suspend fun state(): CoachStateEntity?

    /** Saves the coach's state. Not @Upsert; see [GateBestDao]. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceState(state: CoachStateEntity)

    /** The workout in progress, or null. Emits again when it changes. */
    @Query("SELECT * FROM workout_draft WHERE id = $COACH_ROW_ID")
    fun observeDraft(): Flow<WorkoutDraftEntity?>

    /** Saves the workout in progress, replacing any other. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceDraft(draft: WorkoutDraftEntity)

    /** Removes the workout in progress. */
    @Query("DELETE FROM workout_draft")
    suspend fun clearDraft()
}
