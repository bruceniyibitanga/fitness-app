package app.levitx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

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

    /** Number of finished sessions. Used by tests and, later, the history screen. */
    @Query("SELECT COUNT(*) FROM training_session")
    suspend fun count(): Int
}
