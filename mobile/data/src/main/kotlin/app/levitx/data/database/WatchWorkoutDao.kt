package app.levitx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Reads and writes what the watch keeps for the coach: the next session, and finished workouts on their way. */
@Dao
internal interface WatchWorkoutDao {
    /** The next session's row, or null before the phone has sent one. Emits again when it changes. */
    @Query("SELECT * FROM next_session WHERE id = $COACH_ROW_ID")
    fun observeNext(): Flow<NextSessionEntity?>

    /** Saves the next session's row, replacing any other. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceNext(next: NextSessionEntity)

    /** Queues a finished workout. Queuing one that is already queued changes nothing. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun queue(entry: WorkoutOutboxEntity)

    /** Queued workouts, oldest first. Emits again whenever the queue changes. */
    @Query("SELECT * FROM workout_outbox ORDER BY queuedAtEpochMillis, id")
    fun observeQueued(): Flow<List<WorkoutOutboxEntity>>

    /** Takes a workout off the queue once the other device has it. */
    @Query("DELETE FROM workout_outbox WHERE id = :workoutId")
    suspend fun remove(workoutId: String)
}
