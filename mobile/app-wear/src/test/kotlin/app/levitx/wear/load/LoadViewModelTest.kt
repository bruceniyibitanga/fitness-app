package app.levitx.wear.load

import app.levitx.core.session.SetLoad
import app.levitx.data.equipment.Band
import app.levitx.testing.FakeEquipmentRepository
import app.levitx.wear.MainDispatcherRule
import app.levitx.wear.WatchFakes
import app.levitx.wear.session.WorkoutState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoadViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val red = Band("b1", "Red", 20.0)
    private val controller = WatchFakes().controller()
    private val viewModel by lazy { LoadViewModel(controller, FakeEquipmentRepository(bands = listOf(red))) }

    @Test
    fun startsAtBodyweightWithThePhonesBands() = runTest {
        val state = currentState()
        assertEquals(LoadUiState(bands = listOf(red)), state)
        assertFalse(state.canDecrease)
    }

    @Test
    fun addedWeightStepsUpAndDownAndNeverBelowZero() = runTest {
        currentState()
        viewModel.increaseAdded(STEP_KG)
        viewModel.increaseAdded(STEP_KG)
        assertEquals(2 * STEP_KG, currentState().addedKg, 0.0)

        repeat(3) { viewModel.decreaseAdded(STEP_KG) }
        assertEquals(0.0, currentState().addedKg, 0.0)
    }

    @Test
    fun choosingABandKeepsTheAddedWeight() = runTest {
        currentState()
        viewModel.increaseAdded(STEP_KG)
        viewModel.chooseBand(red)

        assertEquals(WorkoutState.Idle(SetLoad(addedKg = STEP_KG, assistanceKg = 20.0)), controller.state.value)

        viewModel.chooseBand(null)
        assertEquals(0.0, currentState().assistanceKg, 0.0)
        assertEquals(STEP_KG, currentState().addedKg, 0.0)
    }

    /** Subscribes like a screen would, then returns the latest state. */
    private fun TestScope.currentState(): LoadUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel.uiState.value
    }
}

/** One tap in kilograms, as the screen passes it for someone who reads kg. */
private const val STEP_KG = 1.0
