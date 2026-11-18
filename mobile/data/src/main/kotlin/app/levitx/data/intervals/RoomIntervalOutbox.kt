package app.levitx.data.intervals

import app.levitx.data.database.IntervalOutboxEntity
import app.levitx.data.database.LevitxDatabaseProvider
import app.levitx.data.time.TrainingClock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Keeps finished interval workouts in the encrypted database until the phone has them. */
@Singleton
internal class RoomIntervalOutbox @Inject constructor(
    private val databaseProvider: LevitxDatabaseProvider,
    private val clock: TrainingClock,
) : IntervalOutbox {
    override suspend fun queue(workout: QueuedIntervalWorkout) {
        databaseProvider.database().intervalOutboxDao().queue(
            IntervalOutboxEntity(
                id = workout.id,
                json = IntervalCodec.encode(workout.sessions),
                queuedAtEpochMillis = clock.currentTimeMillis(),
            ),
        )
    }

    override fun observeQueued(): Flow<List<QueuedIntervalWorkout>> = flow {
        emitAll(databaseProvider.database().intervalOutboxDao().observeQueued())
    }
        .map { rows ->
            // A row this version can't read is left queued but not sent; it can't be rebuilt anyway.
            rows.mapNotNull { row -> IntervalCodec.decodeOrNull(row.json)?.let { QueuedIntervalWorkout(row.id, it) } }
        }
        .distinctUntilChanged()

    override suspend fun markDelivered(workoutId: String) {
        databaseProvider.database().intervalOutboxDao().remove(workoutId)
    }
}
