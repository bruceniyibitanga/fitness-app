package app.levitx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Reads and writes the queue of sessions waiting to reach the other device. */
@Dao
internal interface SessionOutboxDao {
    /** Queues a session. Queuing one that is already queued changes nothing. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun queue(entry: SessionOutboxEntity)

    /** Takes a session off the queue. The session itself is kept. */
    @Query("DELETE FROM session_outbox WHERE sessionId = :sessionId")
    suspend fun remove(sessionId: String)

    /** Queued sessions with their sets, oldest first. Emits again whenever the queue changes. */
    @Transaction
    @Query(
        "SELECT training_session.* FROM training_session " +
            "INNER JOIN session_outbox ON session_outbox.sessionId = training_session.id " +
            "ORDER BY session_outbox.queuedAtEpochMillis, training_session.id",
    )
    fun observeQueued(): Flow<List<SessionWithSets>>
}
