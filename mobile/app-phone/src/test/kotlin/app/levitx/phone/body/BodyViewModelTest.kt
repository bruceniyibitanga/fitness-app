package app.levitx.phone.body

import app.levitx.core.coach.Equipment
import app.levitx.core.coach.WeighIn
import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeEquipmentRepository
import app.levitx.testing.FakeTrainingClock
import app.levitx.testing.coachState
import app.levitx.testing.newcomer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// UnconfinedTestDispatcher is still marked experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class BodyViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val clock = FakeTrainingClock()
    private val day = 24L * 60 * 60 * 1_000
    private val equipment = FakeEquipmentRepository()

    // Five weigh-ins over twelve days, losing a little: enough for a trend.
    private val weighIns = listOf(76.0, 75.8, 75.7, 75.5, 75.4).mapIndexed { i, kg ->
        WeighIn(clock.now - (12 - i * 3) * day, kg)
    }
    private val coach = FakeCoachRepository(coachState(newcomer().copy(weighIns = weighIns)))

    // Lazy, so it is built after the rule has swapped in the test main dispatcher.
    private val viewModel by lazy { BodyViewModel(equipment, coach, clock) }

    private val ready: BodyUiState.Ready
        get() = viewModel.uiState.value as BodyUiState.Ready

    @Test
    fun showsTheTrendAndWhatItMeansOnTheBar() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val trend = checkNotNull(ready.trend)
        assertTrue("losing weight", checkNotNull(trend.weeklyChange) < 0)
        assertTrue("something to show on the bar", ready.onTheBar.isNotEmpty())
        ready.onTheBar.forEach { load ->
            assertEquals(trend.currentKg * load.bodyweightShare, load.kgPerRep, 0.01)
            assertTrue("${load.name} gets lighter as weight drops", checkNotNull(load.kgPerWeek) < 0)
        }
    }

    @Test
    fun beforeAnyWeighInThereIsNoTrendAndNothingOnTheBar() = runTest {
        val empty = FakeCoachRepository(coachState())
        val viewModel = BodyViewModel(equipment, empty, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value as BodyUiState.Ready
        assertNull(state.trend)
        assertTrue(state.onTheBar.isEmpty())
    }

    @Test
    fun kitIsAddedTakenAwayAndCleared() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        // The newcomer starts with a pull-up bar.
        viewModel.chooseEquipment(Equipment.BOX)
        assertEquals(setOf(Equipment.PULL_UP_BAR, Equipment.BOX), ready.equipment)

        viewModel.chooseEquipment(Equipment.PULL_UP_BAR)
        assertEquals(setOf(Equipment.BOX), ready.equipment)

        viewModel.chooseEquipment(null)
        assertTrue(ready.equipment.isEmpty())
        assertEquals("training days are kept", coachState().trainingDays, checkNotNull(coach.current).trainingDays)
    }

    @Test
    fun savesWeighInsAndBandsAndIgnoresImpossibleValues() = runTest {
        viewModel.saveBodyweight(5.0)
        viewModel.saveBodyweight(Double.NaN)
        assertNull(equipment.observeBodyweightKg().first())

        viewModel.saveBodyweight(74.5)
        assertEquals(74.5, equipment.observeBodyweightKg().first())

        viewModel.addBand(" ", 10.0)
        viewModel.addBand("Red", 0.0)
        assertTrue(equipment.bands.isEmpty())
        viewModel.addBand("Red", 22.5)
        viewModel.deleteBand(equipment.bands.single().id)
        assertTrue(equipment.bands.isEmpty())
    }
}
