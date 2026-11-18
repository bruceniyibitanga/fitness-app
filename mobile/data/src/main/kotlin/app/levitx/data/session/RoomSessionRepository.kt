package app.levitx.data.session

import androidx.room.withTransaction
import app.levitx.core.session.CompletedSession
import app.levitx.core.session.LoggedSet
import app.levitx.data.database.LevitxDatabase
import app.levitx.data.database.LevitxDatabaseProvider
import app.levitx.data.database.LoggedSetEntity
import app.levitx.data.database.SessionOutboxEntity
import app.levitx.data.database.SessionWithSets
import app.levitx.data.database.TrainingSessionEntity
import app.levitx.data.time.TrainingClock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Room's "nothing was inserted" row id, returned when an insert is ignored. */
private const val NOT_INSERTED = -1L

/**
 * Stores sessions, and the queue of sessions waiting to sync, in the encrypted
 * database. Nothing is stored as finishing after this device's clock: a
 * session that says it did was timed by a clock that was ahead, and is moved
 * back to end now, keeping its length.
 */
@Singleton
internal class RoomSessionRepository @Inject constructor(
    private val databaseProvider: LevitxDatabaseProvider,
    private val clock: TrainingClock,
) : SessionRepository,
    SessionOutbox {
    override suspend fun saveCompletedSession(session: CompletedSession) {
        val database = databaseProvider.database()
        database.withTransaction { database.insertIfNew(StoredSession(UUID.randomUUID().toString(), session)) }
    }

    override suspend fun saveIntervalWorkout(workoutId: String, sessions: List<CompletedSession>): Boolean {
        val database = databaseProvider.database()
        return database.withTransaction {
            if (database.sessionDao().hasWorkout(workoutId)) return@withTransaction false
            val now = clock.currentTimeMillis()
            sessions.map { it.endingBy(now) }.forEachIndexed { index, session ->
                val rowId = "$workoutId/$index"
                database.sessionDao().insertSession(
                    TrainingSessionEntity(
                        id = rowId,
                        gateId = session.gateId,
                        startedAtEpochMillis = session.startedAtEpochMillis,
                        finishedAtEpochMillis = session.finishedAtEpochMillis,
                        bodyweightKg = null,
                        workoutId = workoutId,
                    ),
                )
                database.sessionDao().insertSets(session.sets.toEntities(rowId))
            }
            true
        }
    }

    override suspend fun saveReceivedSession(received: StoredSession): Boolean {
        val database = databaseProvider.database()
        val dated = received.copy(session = received.session.endingBy(clock.currentTimeMillis()))
        return database.withTransaction { database.insertIfNew(dated) }
    }

    override suspend fun saveAndQueue(queued: StoredSession) {
        val database = databaseProvider.database()
        database.withTransaction {
            // Queue it only when it is new. A session saved before has either
            // been queued already or has been delivered and taken off the queue.
            if (database.insertIfNew(queued)) {
                database.sessionOutboxDao().queue(
                    SessionOutboxEntity(sessionId = queued.id, queuedAtEpochMillis = clock.currentTimeMillis()),
                )
            }
        }
    }

    override fun observeQueued(): Flow<List<StoredSession>> = flow {
        emitAll(databaseProvider.database().sessionOutboxDao().observeQueued())
    }
        .map { rows -> rows.map { it.toStoredSession() } }
        .distinctUntilChanged()

    override suspend fun markDelivered(sessionId: String) {
        databaseProvider.database().sessionOutboxDao().remove(sessionId)
    }
}

/**
 * Inserts the session, its sets and any new best, unless the id is already
 * stored. Call inside a transaction.
 *
 * @return True when the session was new.
 */
private suspend fun LevitxDatabase.insertIfNew(stored: StoredSession): Boolean {
    val session = stored.session
    val rowId = sessionDao().insertSessionIfNew(
        TrainingSessionEntity(
            id = stored.id,
            gateId = session.gateId,
            startedAtEpochMillis = session.startedAtEpochMillis,
            finishedAtEpochMillis = session.finishedAtEpochMillis,
            bodyweightKg = session.bodyweightKg,
        ),
    )
    if (rowId == NOT_INSERTED) return false
    sessionDao().insertSets(session.sets.toEntities(stored.id))
    // An all-assisted session has no best to record.
    if (session.best > 0) {
        gateBestDao().recordBest(
            gateId = session.gateId,
            best = session.best,
            achievedAtEpochMillis = session.finishedAtEpochMillis,
        )
    }
    return true
}

/** [this] sets as rows of the session [sessionId], in order. */
private fun List<LoggedSet>.toEntities(sessionId: String): List<LoggedSetEntity> = mapIndexed { index, set ->
    LoggedSetEntity(
        sessionId = sessionId,
        position = index,
        value = set.value,
        repsInReserve = set.repsInReserve,
        isBestEffort = set.isBestEffort,
        addedKg = set.addedKg,
        assistanceKg = set.assistanceKg,
    )
}

private fun SessionWithSets.toStoredSession() = StoredSession(
    id = session.id,
    session = CompletedSession(
        gateId = session.gateId,
        startedAtEpochMillis = session.startedAtEpochMillis,
        finishedAtEpochMillis = session.finishedAtEpochMillis,
        sets = sets.sortedBy { it.position }.map { set ->
            LoggedSet(
                value = set.value,
                repsInReserve = set.repsInReserve,
                isBestEffort = set.isBestEffort,
                addedKg = set.addedKg,
                assistanceKg = set.assistanceKg,
            )
        },
        bodyweightKg = session.bodyweightKg,
    ),
)
