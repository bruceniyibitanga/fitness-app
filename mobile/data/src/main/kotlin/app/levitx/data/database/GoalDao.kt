package app.levitx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Reads and writes the single [ActiveGoalEntity] row. */
@Dao
internal interface GoalDao {
    /** The active goal, or null before one is chosen. Emits again when it changes. */
    @Query("SELECT * FROM active_goal WHERE id = $ACTIVE_GOAL_ROW_ID")
    fun observe(): Flow<ActiveGoalEntity?>

    /** Saves [goal], replacing the previous one. Not @Upsert; see [GateBestDao]. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(goal: ActiveGoalEntity)
}
