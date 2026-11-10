package app.levitx.data.progress

import app.levitx.core.skills.PathProgress
import app.levitx.core.skills.SkillPath
import kotlinx.coroutines.flow.Flow

/**
 * The person's best result for each gate, and their progress along skill paths.
 *
 * This is the only way the UI reads or changes progress, so how it is stored
 * can change without touching any screen.
 */
interface ProgressRepository {
    /**
     * Emits progress along [path] straight away, then again whenever a best
     * result changes. Never emits the same progress twice in a row.
     */
    fun observeProgress(path: SkillPath): Flow<PathProgress>

    /**
     * Records [best] reps or seconds for [gateId]. Keeps the stored value if
     * it is already higher, so a weaker attempt never lowers a best.
     */
    suspend fun recordBest(gateId: String, best: Int)
}
