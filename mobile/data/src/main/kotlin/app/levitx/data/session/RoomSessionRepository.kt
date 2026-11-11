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

/** Stores sessions, and the queue of sessions waiting to sync, in the encrypted database. */
@Singleton
internal class RoomSessionRepository @Inject constructor(private val databaseProvider: LevitxDatabaseProvider) :
    SessionRepository,
    SessionOutbox {
    override suspend fun saveCompletedSession(session: CompletedSession) {
        val database = databaseProvider.database()
        database.withTransaction { database.insertIfNew(StoredSession(UUID.randomUUID().toString(), session)) }
    }

    override suspend fun saveReceivedSession(received: StoredSession): Boolean {
        val database = databaseProvider.database()
        return database.withTransaction { database.insertIfNew(received) }
    }

    override suspend fun saveAndQueue(queued: StoredSession) {
        val database = databaseProvider.database()
        database.withTransaction {
            // Queue it only when it is new. A session saved before has either
            // been queued already or has been delivered and taken off the queue.
            if (database.insertIfNew(queued)) {
                database.sessionOutboxDao().queue(
                    SessionOutboxEntity(sessionId = queued.id, queuedAtEpochMillis = System.currentTimeMillis()),
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
    sessionDao().insertSets(
        session.sets.mapIndexed { index, set ->
            LoggedSetEntity(
                sessionId = stored.id,
                position = index,
                value = set.value,
                repsInReserve = set.repsInReserve,
                isBestEffort = set.isBestEffort,
                addedKg = set.addedKg,
                assistanceKg = set.assistanceKg,
            )
        },
    )
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
