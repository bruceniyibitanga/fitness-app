package app.levitx.data.coach

import androidx.room.withTransaction
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.WorkoutRun
import app.levitx.data.database.LevitxDatabaseProvider
import app.levitx.data.database.NextSessionEntity
import app.levitx.data.database.WorkoutOutboxEntity
import app.levitx.data.time.TrainingClock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Keeps the next session in the encrypted database. */
@Singleton
internal class RoomNextSessionRepository @Inject constructor(private val databaseProvider: LevitxDatabaseProvider) :
    NextSessionRepository {
    override fun observeNext(): Flow<PlannedSession?> = flow {
        emitAll(databaseProvider.database().watchWorkoutDao().observeNext())
    }
        // A plan this version can't read is treated as no plan: the phone sends a new one.
        .map { row -> row?.json?.let(WorkoutCodec::decodePlanOrNull) }
        .distinctUntilChanged()

    override suspend fun awaitingWorkoutId(): String? =
        databaseProvider.database().watchWorkoutDao().observeNext().first()?.awaitingWorkoutId

    override suspend fun replace(session: PlannedSession?) {
        databaseProvider.database().watchWorkoutDao()
            .replaceNext(NextSessionEntity(json = session?.let(WorkoutCodec::encodePlan)))
    }
}

/** Keeps finished workouts in the encrypted database until the phone confirms them. */
@Singleton
internal class RoomWorkoutOutbox @Inject constructor(
    private val databaseProvider: LevitxDatabaseProvider,
    private val clock: TrainingClock,
) : WorkoutOutbox {
    override suspend fun finishAndQueue(run: WorkoutRun, finishedAtEpochMillis: Long): Map<String, Int> {
        val database = databaseProvider.database()
        val result = run.result(finishedAtEpochMillis)
        return database.withTransaction {
            var raised = emptyMap<String, Int>()
            if (result != null) {
                database.watchWorkoutDao().queue(
                    WorkoutOutboxEntity(
                        id = run.id,
                        json = WorkoutCodec.encodeRun(run),
                        finishedAtEpochMillis = result.finishedAtEpochMillis,
                        queuedAtEpochMillis = clock.currentTimeMillis(),
                    ),
                )
                val before = result.gateBests.keys.associateWith { database.gateBestDao().best(it) ?: 0 }
                raised = result.newGateBests(before)
                // recordBest keeps the higher value, so the phone's figure later can't be lowered by this.
                raised.forEach { (gateId, best) ->
                    database.gateBestDao().recordBest(gateId, best, result.finishedAtEpochMillis)
                }
                // This session is done. The next one is whatever the phone plans once it has this workout.
                database.watchWorkoutDao().replaceNext(NextSessionEntity(json = null, awaitingWorkoutId = run.id))
            }
            database.coachDao().clearDraft()
            raised
        }
    }

    override fun observeQueued(): Flow<List<QueuedWorkout>> = flow {
        emitAll(databaseProvider.database().watchWorkoutDao().observeQueued())
    }
        .map { rows ->
            // A row this version can't read is left queued but not sent; it can't be rebuilt anyway.
            rows.mapNotNull { row ->
                WorkoutCodec.decodeRunOrNull(row.json)?.let { QueuedWorkout(it, row.finishedAtEpochMillis) }
            }
        }
        .distinctUntilChanged()

    override suspend fun markDelivered(workoutId: String) {
        databaseProvider.database().watchWorkoutDao().remove(workoutId)
    }
}
