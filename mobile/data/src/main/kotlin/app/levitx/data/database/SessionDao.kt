package app.levitx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Writes finished sessions and their sets. */
@Dao
internal interface SessionDao {
    /** Saves a new session. Fails if the id already exists, which would be a bug. */
    @Insert
    suspend fun insertSession(session: TrainingSessionEntity)

    /**
     * Saves a session unless one with the same id is already stored.
     *
     * @return The new row id, or -1 when the session was already there.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessionIfNew(session: TrainingSessionEntity): Long

    /** Saves the sets of a session already inserted. */
    @Insert
    suspend fun insertSets(sets: List<LoggedSetEntity>)

    /** True when the coach workout [workoutId] is already stored. */
    @Query("SELECT EXISTS(SELECT 1 FROM training_session WHERE workoutId = :workoutId)")
    suspend fun hasWorkout(workoutId: String): Boolean

    /** Ids of the most recently finished workouts, newest first. Emits again when one is stored. */
    @Query(
        "SELECT workoutId FROM training_session WHERE workoutId IS NOT NULL " +
            "GROUP BY workoutId ORDER BY MAX(finishedAtEpochMillis) DESC, workoutId LIMIT :limit",
    )
    fun observeRecentWorkoutIds(limit: Int): Flow<List<String>>

    /**
     * Every finished session with its sets, newest first. Emits again when one
     * is stored. In a transaction so the sets always match their sessions.
     */
    @Transaction
    @Query("SELECT * FROM training_session ORDER BY finishedAtEpochMillis DESC")
    fun observeAllWithSets(): Flow<List<SessionWithSets>>

    /** Number of finished sessions. Used by tests. */
    @Query("SELECT COUNT(*) FROM training_session")
    suspend fun count(): Int
}
