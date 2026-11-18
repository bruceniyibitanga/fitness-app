package app.levitx.data.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A cardio or mobility workout finished on the watch that the phone hasn't
 * confirmed yet (added in version 7). It is deleted once confirmed.
 *
 * @property id The workout's id, a random UUID.
 * @property json The work done; see `IntervalCodec` in the `intervals` package.
 * @property queuedAtEpochMillis When it was queued, for sending oldest first.
 */
@Entity(tableName = "interval_outbox")
internal data class IntervalOutboxEntity(@PrimaryKey val id: String, val json: String, val queuedAtEpochMillis: Long)

/** Reads and writes the watch's queue of finished interval workouts. */
@Dao
internal interface IntervalOutboxDao {
    /** Queues a workout. Queuing one that is already queued changes nothing. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun queue(entry: IntervalOutboxEntity)

    /** Queued workouts, oldest first. Emits again whenever the queue changes. */
    @Query("SELECT * FROM interval_outbox ORDER BY queuedAtEpochMillis, id")
    fun observeQueued(): Flow<List<IntervalOutboxEntity>>

    /** Deletes a workout once the phone has it. */
    @Query("DELETE FROM interval_outbox WHERE id = :workoutId")
    suspend fun remove(workoutId: String)
}
