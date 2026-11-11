package app.levitx.phone.train

import app.levitx.core.coach.Coach
import app.levitx.core.coach.WorkoutFocus
import app.levitx.core.skills.SkillCatalogue
import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeTrainingClock
import app.levitx.testing.coachState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// UnconfinedTestDispatcher is still marked experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class TrainViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun listsEveryProgramAndAWorkoutForEveryFocus() = runTest {
        val viewModel = TrainViewModel(FakeCoachRepository(coachState()), Coach(), FakeTrainingClock())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val ready = viewModel.uiState.value as TrainUiState.Ready

        assertEquals(SkillCatalogue.all.map { it.id }, ready.programs.map { it.path.id })
        assertTrue(ready.programs.single { it.isCurrent }.path.id == "first_pull_up")
        assertEquals(WorkoutFocus.entries, ready.workouts.map { it.focus })
        ready.workouts.forEach { assertNotNull("${it.focus} had nothing", it.session) }
    }
}
