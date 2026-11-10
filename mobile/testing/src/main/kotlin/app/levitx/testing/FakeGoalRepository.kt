package app.levitx.testing

import app.levitx.core.skills.SkillPath
import app.levitx.data.goal.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [GoalRepository]. */
class FakeGoalRepository(initial: SkillPath? = null) : GoalRepository {
    private val goal = MutableStateFlow(initial)

    override fun observeActiveGoal(): Flow<SkillPath?> = goal.asStateFlow()

    override suspend fun setActiveGoal(path: SkillPath) {
        goal.value = path
    }

    /** The current goal, for assertions. */
    val current: SkillPath?
        get() = goal.value
}
