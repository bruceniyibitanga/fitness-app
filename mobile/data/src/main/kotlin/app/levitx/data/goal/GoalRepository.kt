package app.levitx.data.goal

import app.levitx.core.skills.SkillPath
import kotlinx.coroutines.flow.Flow

/** The skill path the person has chosen to work toward. One at a time. */
interface GoalRepository {
    /** Emits the active goal straight away and whenever it changes. Null before one is chosen. */
    fun observeActiveGoal(): Flow<SkillPath?>

    /** Makes [path] the active goal, replacing any previous one. Bests are kept. */
    suspend fun setActiveGoal(path: SkillPath)
}
