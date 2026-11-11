package app.levitx.phone.program

import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.coachState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// UnconfinedTestDispatcher is still marked experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class ProgramViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val coachRepository = FakeCoachRepository(coachState())

    @Test
    fun makingAProgramThePlanChangesOnlyTheGoal() = runTest {
        val before = checkNotNull(coachRepository.current)
        val viewModel = ProgramViewModel("muscle_up", coachRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertFalse((viewModel.uiState.value as ProgramUiState.Ready).isPlan)

        assertTrue(viewModel.makePlan())
        val after = checkNotNull(coachRepository.current)
        assertEquals("muscle_up", after.model.profile.goalPathId)
        assertEquals(before.model.profile.copy(goalPathId = "muscle_up"), after.model.profile)
        assertEquals(before.trainingDays, after.trainingDays)
        assertTrue((viewModel.uiState.value as ProgramUiState.Ready).isPlan)
    }

    @Test
    fun anUnknownProgramCantBeMadeThePlan() = runTest {
        val viewModel = ProgramViewModel("nope", coachRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(ProgramUiState.NotFound, viewModel.uiState.value)
        assertFalse(viewModel.makePlan())
        assertEquals("first_pull_up", coachRepository.current?.model?.profile?.goalPathId)
    }
}
