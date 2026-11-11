package app.levitx.phone.session

import androidx.lifecycle.SavedStateHandle
import app.levitx.core.session.SessionPlanner
import app.levitx.core.skills.SkillCatalogue
import app.levitx.data.equipment.Band
import app.levitx.data.time.TrainingClock
import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeEquipmentRepository
import app.levitx.testing.FakeGoalRepository
import app.levitx.testing.FakeProgressRepository
import app.levitx.testing.FakeSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SessionViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val path = SkillCatalogue.muscleUp
    private val dips = path.gates[1]
    private val clock = FakeClock()
    private val progress = FakeProgressRepository(mapOf(path.gates[0].id to 30, dips.id to 6))
    private val sessions = FakeSessionRepository(progress)
    private val equipment = FakeEquipmentRepository(bodyweightKg = 74.0, bands = listOf(Band("red", "Red", 20.0)))

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        SessionViewModel(FakeGoalRepository(path), progress, sessions, equipment, clock, savedState)

    @Test
    fun startsAtTheFirstSetOfTodaysPlan() = runTest {
        val state = viewModel().uiState.value as SessionUiState.Active
        assertEquals(SessionPlanner.planFor(dips, best = 6), state.plan)
        assertEquals(0, state.setIndex)
        assertNull(state.restEndsAtElapsedMillis)
    }

    @Test
    fun loggingASetStartsRestOnTheElapsedClock() = runTest {
        val viewModel = viewModel()
        viewModel.logSet(4, repsInReserve = 2)
        val state = viewModel.uiState.value as SessionUiState.Active
        assertEquals(clock.elapsed + SessionPlanner.REST_SECONDS_REPS * 1_000L, state.restEndsAtElapsedMillis)
        assertEquals(1, state.setIndex)
    }

    @Test
    fun cantLogDuringRest() = runTest {
        val viewModel = viewModel()
        viewModel.logSet(4, 2)
        viewModel.logSet(4, 2)
        assertEquals(1, (viewModel.uiState.value as SessionUiState.Active).logged.size)
    }

    @Test
    fun finishingEverySetSavesTheSessionAndRaisesTheBest() = runTest {
        val viewModel = viewModel()
        repeat(SessionPlanner.WORK_SETS) {
            viewModel.logSet(4, 2)
            viewModel.endRest()
        }
        viewModel.logSet(8, 0)

        val finished = viewModel.uiState.value as SessionUiState.Finished
        assertEquals(6, finished.previousBest)
        assertEquals(8, finished.sessionBest)
        assertTrue(finished.isNewBest)
        assertEquals(1, sessions.sessions.size)
        assertEquals(8, progress.current[dips.id])
    }

    @Test
    fun endingEarlySavesWhatWasDone() = runTest {
        val viewModel = viewModel()
        viewModel.logSet(5, 1)
        viewModel.finish()
        assertEquals(listOf(5), sessions.sessions.single().sets.map { it.value })
    }

    @Test
    fun endingBeforeAnySetSavesNothing() = runTest {
        val viewModel = viewModel()
        viewModel.finish()
        assertEquals(SessionUiState.NothingToTrain, viewModel.uiState.value)
        assertTrue(sessions.sessions.isEmpty())
    }

    @Test
    fun setsSurviveTheAppBeingKilled() = runTest {
        val savedState = SavedStateHandle()
        viewModel(savedState).logSet(4, 2)
        // A new ViewModel with the same saved state is what Android gives back after process death.
        val restored = viewModel(savedState).uiState.value as SessionUiState.Active
        assertEquals(1, restored.logged.size)
        assertNotNull(restored.restEndsAtElapsedMillis)
    }

    @Test
    fun completedPathHasNothingToTrain() = runTest {
        path.gates.forEach { progress.recordBest(it.id, it.target) }
        assertEquals(SessionUiState.NothingToTrain, viewModel().uiState.value)
    }

    @Test
    fun bandAssistedBestDoesNotRaiseTheGate() = runTest {
        val viewModel = viewModel()
        repeat(SessionPlanner.WORK_SETS) {
            viewModel.logSet(4, 2)
            viewModel.endRest()
        }
        viewModel.logSet(12, 0, assistanceKg = 20.0)

        val finished = viewModel.uiState.value as SessionUiState.Finished
        assertEquals(4, finished.sessionBest)
        assertTrue(finished.usedBand)
        assertEquals(6, progress.current[dips.id])
    }

    @Test
    fun loadAndBodyweightAreSavedWithTheSession() = runTest {
        val viewModel = viewModel()
        viewModel.logSet(5, 2, addedKg = 10.0)
        viewModel.finish()
        val saved = sessions.sessions.single()
        assertEquals(74.0, saved.bodyweightKg)
        assertEquals(10.0, saved.sets.single().addedKg, 0.0)
    }

    @Test
    fun activeStateCarriesBodyweightAndBands() = runTest {
        val state = viewModel().uiState.value as SessionUiState.Active
        assertEquals(74.0, state.bodyweightKg)
        assertEquals(listOf("red"), state.bands.map { it.id })
        assertTrue(state.tracksLoad)
    }

    @Test
    fun aSessionKilledOnTheLastRestKeepsItsPlanAndStart() = runTest {
        val savedState = SavedStateHandle()
        val first = viewModel(savedState)
        first.logSet(4, 2, addedKg = 5.0)
        val before = first.uiState.value as SessionUiState.Active

        val restored = viewModel(savedState).uiState.value as SessionUiState.Active

        assertEquals(before.plan, restored.plan)
        assertEquals(before.logged, restored.logged)
        assertEquals(5.0, restored.logged.single().addedKg, 0.0)
    }

    @Test
    fun impossibleLoadFiguresAreClampedInsteadOfCrashing() = runTest {
        val viewModel = viewModel()
        viewModel.logSet(5, 2, addedKg = Double.NaN, assistanceKg = -3.0)
        val set = (viewModel.uiState.value as SessionUiState.Active).logged.single()
        assertEquals(0.0, set.addedKg, 0.0)
        assertEquals(0.0, set.assistanceKg, 0.0)
    }

    @Test
    fun finishingTwiceSavesOnce() = runTest {
        val viewModel = viewModel()
        viewModel.logSet(5, 1)
        viewModel.finish()
        viewModel.finish()
        assertEquals(1, sessions.sessions.size)
    }

    private class FakeClock : TrainingClock {
        val elapsed = 50_000L

        override fun elapsedRealtimeMillis(): Long = elapsed

        override fun currentTimeMillis(): Long = 1_000_000L
    }
}
