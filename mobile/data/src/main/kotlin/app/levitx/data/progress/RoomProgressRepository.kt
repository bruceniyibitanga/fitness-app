package app.levitx.data.progress

import app.levitx.core.skills.PathProgress
import app.levitx.core.skills.SkillPath
import app.levitx.data.database.LevitxDatabaseProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Stores bests in the encrypted database.
 *
 * Room runs queries on its own background threads and re-emits Flow queries
 * when the table changes, so progress on screen updates by itself after a
 * new best is recorded.
 */
@Singleton
internal class RoomProgressRepository @Inject constructor(private val databaseProvider: LevitxDatabaseProvider) :
    ProgressRepository {
    override fun observeProgress(path: SkillPath): Flow<PathProgress> = flow {
        emitAll(databaseProvider.database().gateBestDao().observeAll())
    }
        .map { rows -> path.progressFor(rows.associate { it.gateId to it.best }) }
        .distinctUntilChanged()

    override suspend fun recordBest(gateId: String, best: Int) {
        databaseProvider.database().gateBestDao().recordBest(
            gateId = gateId,
            best = best,
            achievedAtEpochMillis = System.currentTimeMillis(),
        )
    }
}
