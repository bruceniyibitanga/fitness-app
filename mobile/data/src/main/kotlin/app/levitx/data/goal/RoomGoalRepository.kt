package app.levitx.data.goal

import app.levitx.core.skills.SkillCatalogue
import app.levitx.core.skills.SkillPath
import app.levitx.data.database.ActiveGoalEntity
import app.levitx.data.database.LevitxDatabaseProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Stores the active goal's path id in the encrypted database.
 *
 * Only the id is stored; the path itself comes from `SkillCatalogue`. If a
 * stored id no longer exists in the catalogue, there is no active goal and
 * the person is asked to choose again, rather than the app crashing.
 */
@Singleton
internal class RoomGoalRepository @Inject constructor(private val databaseProvider: LevitxDatabaseProvider) :
    GoalRepository {
    override fun observeActiveGoal(): Flow<SkillPath?> = flow {
        emitAll(databaseProvider.database().goalDao().observe())
    }
        .map { row -> row?.let { SkillCatalogue.byId(it.pathId) } }
        .distinctUntilChanged()

    override suspend fun setActiveGoal(path: SkillPath) {
        databaseProvider.database().goalDao().replace(
            ActiveGoalEntity(pathId = path.id, chosenAtEpochMillis = System.currentTimeMillis()),
        )
    }
}
