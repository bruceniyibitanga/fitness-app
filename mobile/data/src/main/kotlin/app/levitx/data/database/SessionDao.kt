package app.levitx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Writes finished sessions and their sets. */
@Dao
internal interface SessionDao {
    /** Saves a new session. Fails if the id already exists, which would be a bug. */
    @Insert
    suspend fun insertSession(session: TrainingSessionEntity)

    /** Saves the sets of a session already inserted. */
    @Insert
    suspend fun insertSets(sets: List<LoggedSetEntity>)

    /** Number of finished sessions. Used by tests and, later, the history screen. */
    @Query("SELECT COUNT(*) FROM training_session")
    suspend fun count(): Int
}
