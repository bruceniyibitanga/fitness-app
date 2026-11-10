package app.levitx.data.progress

import app.levitx.data.database.LevitxDatabaseProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores bests in the encrypted database.
 *
 * Room runs queries on its own background threads and re-emits Flow queries
 * when the table changes, so screens update by themselves after a new best.
 */
@Singleton
internal class RoomProgressRepository @Inject constructor(
    private val databaseProvider: LevitxDatabaseProvider,
) : ProgressRepository {
    override fun observeBests(): Flow<Map<String, Int>> = flow {
        emitAll(databaseProvider.database().gateBestDao().observeAll())
    }
        .map { rows -> rows.associate { it.gateId to it.best } }
        .distinctUntilChanged()

    override suspend fun recordBest(gateId: String, best: Int) {
        databaseProvider.database().gateBestDao().recordBest(
            gateId = gateId,
            best = best,
            achievedAtEpochMillis = System.currentTimeMillis(),
        )
    }
}
