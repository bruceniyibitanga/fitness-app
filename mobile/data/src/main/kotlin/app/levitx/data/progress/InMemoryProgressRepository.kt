package app.levitx.data.progress

import app.levitx.core.skills.PathProgress
import app.levitx.core.skills.SkillPath
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Keeps bests in memory. They are lost when the app closes.
 *
 * The apps use [RoomProgressRepository]; this is the fake for tests, so
 * ViewModel tests run on the JVM without a device or database.
 */
@Singleton
class InMemoryProgressRepository @Inject constructor() : ProgressRepository {
    private val bestByGateId = MutableStateFlow<Map<String, Int>>(emptyMap())

    override fun observeProgress(path: SkillPath): Flow<PathProgress> = bestByGateId
        .map { bests -> path.progressFor(bests) }
        .distinctUntilChanged()

    override suspend fun recordBest(gateId: String, best: Int) {
        // update() retries if another coroutine changed the map in between,
        // so two results recorded at once can't overwrite each other.
        bestByGateId.update { bests ->
            if (best > (bests[gateId] ?: 0)) bests + (gateId to best) else bests
        }
    }
}
