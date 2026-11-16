package app.levitx.data.privacy

import app.levitx.data.IoDispatcher
import app.levitx.data.database.LevitxDatabaseProvider
import app.levitx.data.history.toWorkouts
import app.levitx.data.time.TrainingClock
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Exports and deletes the person's data in the encrypted database. Both run
 * on [ioDispatcher]: they read or write every table.
 */
@Singleton
internal class RoomPrivacyRepository @Inject constructor(
    private val databaseProvider: LevitxDatabaseProvider,
    private val clock: TrainingClock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PrivacyRepository {
    override suspend fun export(out: OutputStream) {
        val database = databaseProvider.database()
        withContext(ioDispatcher) {
            val snapshot = ExportSnapshot(
                coach = database.coachDao().state()?.json?.let { Json.parseToJsonElement(it) },
                workouts = toWorkouts(database.sessionDao().allWithSets()),
                weighIns = database.equipmentDao().bodyweightSince(Long.MIN_VALUE)
                    .map { ExportWeighIn(isoTime(it.recordedAtEpochMillis), it.kg) },
                bands = database.equipmentDao().observeBands().first().map { ExportBand(it.name, it.assistanceKg) },
                gateBests = database.gateBestDao().all().associate { it.gateId to it.best },
            )
            writeExport(snapshot, clock.currentTimeMillis(), out)
        }
    }

    override suspend fun deleteEverything() {
        val database = databaseProvider.database()
        // Room deletes every row, then compacts the file so the space they used is rewritten.
        withContext(ioDispatcher) { database.clearAllTables() }
    }
}
