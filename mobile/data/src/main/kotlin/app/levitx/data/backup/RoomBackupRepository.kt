package app.levitx.data.backup

import androidx.room.withTransaction
import app.levitx.data.DefaultDispatcher
import app.levitx.data.IoDispatcher
import app.levitx.data.database.ActiveGoalEntity
import app.levitx.data.database.BandEntity
import app.levitx.data.database.BodyweightEntity
import app.levitx.data.database.CoachStateEntity
import app.levitx.data.database.GateBestEntity
import app.levitx.data.database.LevitxDatabase
import app.levitx.data.database.LevitxDatabaseProvider
import app.levitx.data.database.LoggedSetEntity
import app.levitx.data.database.RestoreDao
import app.levitx.data.database.TrainingSessionEntity
import app.levitx.data.time.TrainingClock
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

private const val READ_BUFFER_BYTES = 64 * 1024

/**
 * Makes and restores backups of the encrypted database.
 *
 * Reading and writing files runs on [ioDispatcher]. Making the key, encrypting
 * and checking run on [defaultDispatcher]: the key alone takes about a second
 * on a phone, on purpose, so none of it may touch the main thread.
 */
@Singleton
internal class RoomBackupRepository @Inject constructor(
    private val databaseProvider: LevitxDatabaseProvider,
    private val clock: TrainingClock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : BackupRepository {
    override suspend fun write(out: OutputStream, words: List<String>) {
        val database = databaseProvider.database()
        val document = withContext(ioDispatcher) {
            // One transaction, so the backup is a single moment and not half of a workout being saved.
            database.withTransaction { snapshot(database, clock.currentTimeMillis()) }
        }
        val file = withContext(defaultDispatcher) { BackupCipher.encrypt(BackupCodec.encode(document), words) }
        withContext(ioDispatcher) { out.write(file) }
    }

    override suspend fun restore(input: InputStream, words: List<String>): RestoreResult {
        val file = withContext(ioDispatcher) { input.readCapped() }
            ?: return RestoreResult.Failed(BackupProblem.TOO_LARGE)
        return try {
            val document = withContext(defaultDispatcher) {
                BackupCodec.decode(BackupCipher.decrypt(file, words)).checked(clock.currentTimeMillis())
            }
            replaceWith(document)
        } catch (error: BackupException) {
            RestoreResult.Failed(error.problem)
        }
    }

    /** Puts [document] in place of the person's data, unless a workout is in progress. */
    private suspend fun replaceWith(document: BackupDocument): RestoreResult {
        val database = databaseProvider.database()
        return withContext(ioDispatcher) {
            database.withTransaction {
                if (database.restoreDao().hasWorkoutInProgress()) {
                    RestoreResult.WorkoutInProgress
                } else {
                    database.restoreDao().replaceWith(document, clock.currentTimeMillis())
                    RestoreResult.Restored
                }
            }
        }
    }

    /** All of this stream, or null if it runs past [MAX_FILE_BYTES]: a bigger file isn't read into memory. */
    private fun InputStream.readCapped(): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(READ_BUFFER_BYTES)
        var read = read(buffer)
        while (read >= 0) {
            out.write(buffer, 0, read)
            if (out.size() > MAX_FILE_BYTES) return null
            read = read(buffer)
        }
        return out.toByteArray()
    }
}

/** Everything a backup holds, read from [database], stamped [nowEpochMillis]. Run inside a transaction. */
private suspend fun snapshot(database: LevitxDatabase, nowEpochMillis: Long): BackupDocument = BackupDocument(
    v = BACKUP_DOCUMENT_VERSION,
    madeAt = nowEpochMillis,
    coach = database.coachDao().state()?.json,
    goal = database.goalDao().current()?.let { BackedUpGoal(it.pathId, it.chosenAtEpochMillis) },
    weighIns = database.equipmentDao().bodyweightSince(Long.MIN_VALUE)
        .map { BackedUpWeighIn(it.kg, it.recordedAtEpochMillis) },
    bands = database.equipmentDao().bands().map {
        BackedUpBand(it.id, it.name, it.assistanceKg, it.createdAtEpochMillis)
    },
    gateBests = database.gateBestDao().all().map { BackedUpGateBest(it.gateId, it.best, it.achievedAtEpochMillis) },
    sessions = database.sessionDao().allWithSets().map { (session, sets) ->
        BackedUpSession(
            id = session.id,
            gateId = session.gateId,
            startedAt = session.startedAtEpochMillis,
            finishedAt = session.finishedAtEpochMillis,
            bodyweightKg = session.bodyweightKg,
            workoutId = session.workoutId,
            sets = sets.sortedBy { it.position }.map {
                BackedUpSet(it.position, it.value, it.repsInReserve, it.isBestEffort, it.addedKg, it.assistanceKg)
            },
        )
    },
)

/** Replaces the person's data with [document]. Runs inside the caller's transaction. */
private suspend fun RestoreDao.replaceWith(document: BackupDocument, nowEpochMillis: Long) {
    clearSessions()
    clearGateBests()
    clearBands()
    clearWeighIns()
    clearGoal()
    clearCoach()
    clearNextSession()
    insertPlan(
        coach = listOfNotNull(
            document.coach?.let {
                CoachStateEntity(json = it, updatedAtEpochMillis = nowEpochMillis)
            },
        ),
        goal = listOfNotNull(
            document.goal?.let {
                ActiveGoalEntity(pathId = it.pathId, chosenAtEpochMillis = it.chosenAt)
            },
        ),
    )
    insertHistory(
        weighIns = document.weighIns.map { BodyweightEntity(kg = it.kg, recordedAtEpochMillis = it.at) },
        bands = document.bands.map { BandEntity(it.id, it.name, it.assistanceKg, it.createdAt) },
        bests = document.gateBests.map { GateBestEntity(it.gateId, it.best, it.at) },
        sessions = document.sessions.map {
            TrainingSessionEntity(it.id, it.gateId, it.startedAt, it.finishedAt, it.bodyweightKg, it.workoutId)
        },
        sets = document.sessions.flatMap { session ->
            session.sets.map {
                LoggedSetEntity(
                    sessionId = session.id,
                    position = it.position,
                    value = it.value,
                    repsInReserve = it.repsInReserve,
                    isBestEffort = it.isBestEffort,
                    addedKg = it.addedKg,
                    assistanceKg = it.assistanceKg,
                )
            }
        },
    )
}
