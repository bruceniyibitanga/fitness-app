package app.levitx.phone.onboarding

import app.levitx.core.coach.Coach
import app.levitx.core.coach.PullUpAnswer
import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeEquipmentRepository
import app.levitx.testing.FakeTrainingClock
import java.time.DayOfWeek
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val coachRepository = FakeCoachRepository()
    private val equipment = FakeEquipmentRepository()
    private val viewModel = OnboardingViewModel(coachRepository, equipment, Coach(), FakeTrainingClock())

    private val step: OnboardingStep
        get() = viewModel.uiState.value.step

    @Test
    fun aStepWaitsForWhatItNeeds() {
        viewModel.next()
        assertEquals(OnboardingStep.GOAL, step)
        viewModel.next()
        assertEquals(OnboardingStep.GOAL, step)
        viewModel.chooseGoal(null)
        viewModel.next()
        assertEquals(OnboardingStep.HOW, step)
        viewModel.typeBodyweight("abc")
        viewModel.answerPullUp(PullUpAnswer.YES)
        viewModel.next()
        assertEquals(OnboardingStep.HOW, step)
    }

    @Test
    fun backStepsThroughSetupThenLetsTheAppClose() {
        assertFalse(viewModel.back())
        viewModel.next()
        assertTrue(viewModel.back())
        assertEquals(OnboardingStep.WELCOME, step)
    }

    @Test
    fun finishingSetsUpTheCoachAndLogsBodyweight() = runTest {
        viewModel.next()
        viewModel.chooseGoal("first_pull_up")
        viewModel.next()
        viewModel.answerPullUp(PullUpAnswer.NOT_YET)
        viewModel.typeBodyweight("74.5")
        viewModel.next()
        viewModel.toggleDay(DayOfWeek.FRIDAY)
        viewModel.next()
        assertEquals(OnboardingStep.READY, step)
        assertNotNull(viewModel.uiState.value.firstSession)

        assertTrue(viewModel.finish())
        val state = checkNotNull(coachRepository.current)
        assertEquals("first_pull_up", state.model.profile.goalPathId)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), state.trainingDays)
        assertEquals(74.5, equipment.observeBodyweightKg().first())
    }

    @Test
    fun finishingEarlySavesNothing() = runTest {
        assertFalse(viewModel.finish())
        assertNull(coachRepository.current)
    }
}
