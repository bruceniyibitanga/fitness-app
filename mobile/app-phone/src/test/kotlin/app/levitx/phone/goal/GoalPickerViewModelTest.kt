package app.levitx.phone.goal

import app.levitx.core.skills.SkillCatalogue
import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeGoalRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GoalPickerViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun choosingSavesTheGoalThenSignalsOnce() {
        val goals = FakeGoalRepository()
        val viewModel = GoalPickerViewModel(goals)

        viewModel.choose(SkillCatalogue.muscleUp)
        assertEquals(SkillCatalogue.muscleUp, goals.current)
        assertTrue(viewModel.goalSaved.value)

        viewModel.onNavigated()
        assertFalse(viewModel.goalSaved.value)
    }
}
