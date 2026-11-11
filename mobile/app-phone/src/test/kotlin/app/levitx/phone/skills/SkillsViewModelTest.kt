package app.levitx.phone.skills

import app.levitx.core.coach.ExerciseCatalogue
import app.levitx.core.coach.MovementPattern
import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.coachState
import app.levitx.testing.newcomer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// UnconfinedTestDispatcher is still marked experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class SkillsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun showsEveryLadder() = runTest {
        val viewModel = SkillsViewModel(FakeCoachRepository(coachState()))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val ready = viewModel.uiState.value as SkillsUiState.Ready
        assertEquals(ExerciseCatalogue.ladders.map { it.pattern }, ready.ladders.map { it.pattern })
    }

    @Test
    fun theGateBeingWorkedOnTrainsItsMovement() {
        val byPattern = standings(newcomer()).associateBy { it.pattern }
        assertTrue(byPattern.getValue(MovementPattern.VERTICAL_PULL).isTrainedByGate)
        assertFalse(byPattern.getValue(MovementPattern.SQUAT).isTrainedByGate)
    }

    @Test
    fun withoutTheKitThereIsNoExercise() {
        val noKit = newcomer().let { it.copy(profile = it.profile.copy(equipment = emptySet())) }
        val pull = standings(noKit).single { it.pattern == MovementPattern.HORIZONTAL_PULL }
        assertNull(pull.exercise)
        assertNull(pull.next)
    }

    @Test
    fun theTopRungHasNothingNext() {
        val ladder = ExerciseCatalogue.ladder(MovementPattern.SQUAT)
        val model = newcomer().copy(rungs = mapOf(MovementPattern.SQUAT to ladder.rungs.lastIndex))
        val squat = standings(model).single { it.pattern == MovementPattern.SQUAT }
        assertEquals(ladder.rungs.lastIndex, squat.rung)
        assertNull(squat.next)
    }
}
