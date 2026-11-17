package app.levitx.phone.onboarding

import app.levitx.core.coach.Coach
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.MovementPattern
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.QuickTest
import app.levitx.core.coach.TrainingAim
import app.levitx.core.load.WeightUnit
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
    private companion object {
        const val PUSH_UP_RUNG = 4
    }

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val coachRepository = FakeCoachRepository()
    private val equipment = FakeEquipmentRepository()

    // Lazy, so it is built after the rule has swapped in the test main dispatcher.
    private val viewModel by lazy { OnboardingViewModel(coachRepository, equipment, Coach(), FakeTrainingClock()) }

    private val step: OnboardingStep
        get() = viewModel.uiState.value.step

    @Test
    fun aStepWaitsForWhatItNeeds() {
        viewModel.next()
        assertEquals(OnboardingStep.GOAL, step)
        viewModel.next()
        assertEquals(OnboardingStep.GOAL, step)
        viewModel.answer(SetupAnswer.Goal(null))
        viewModel.next()
        assertEquals(OnboardingStep.HOW, step)
        viewModel.answer(SetupAnswer.Bodyweight("abc"))
        viewModel.answer(SetupAnswer.PullUp(PullUpAnswer.YES))
        viewModel.next()
        assertEquals(OnboardingStep.HOW, step)
    }

    @Test
    fun noKitIsAssumedAndNothingYetClearsWhatWasPicked() {
        assertEquals(emptySet<Equipment>(), viewModel.uiState.value.equipment)

        viewModel.answer(SetupAnswer.Kit(Equipment.PULL_UP_BAR))
        viewModel.answer(SetupAnswer.Kit(Equipment.BOX))
        assertEquals(setOf(Equipment.PULL_UP_BAR, Equipment.BOX), viewModel.uiState.value.equipment)

        viewModel.answer(SetupAnswer.Kit(Equipment.BOX))
        assertEquals(setOf(Equipment.PULL_UP_BAR), viewModel.uiState.value.equipment)

        viewModel.answer(SetupAnswer.Kit(null))
        assertEquals(emptySet<Equipment>(), viewModel.uiState.value.equipment)
    }

    @Test
    fun aBarGoalWithoutABarSaysWhatIsMissing() {
        viewModel.answer(SetupAnswer.Goal("muscle_up"))
        assertEquals(setOf(Equipment.PULL_UP_BAR), viewModel.uiState.value.missingGoalKit)

        viewModel.answer(SetupAnswer.Kit(Equipment.PULL_UP_BAR))
        assertEquals(emptySet<Equipment>(), viewModel.uiState.value.missingGoalKit)

        viewModel.answer(SetupAnswer.Goal(null))
        viewModel.answer(SetupAnswer.Kit(null))
        assertEquals(emptySet<Equipment>(), viewModel.uiState.value.missingGoalKit)
    }

    @Test
    fun backStepsThroughSetupThenLetsTheAppClose() {
        assertFalse(viewModel.back())
        viewModel.next()
        assertTrue(viewModel.back())
        assertEquals(OnboardingStep.WELCOME, step)
    }

    @Test
    fun bodyweightTypedInPoundsIsKeptInKilograms() {
        viewModel.answer(SetupAnswer.Bodyweight("165", WeightUnit.LB))
        assertEquals(74.84, checkNotNull(viewModel.uiState.value.bodyweightKg), 0.01)
        viewModel.answer(SetupAnswer.Bodyweight("30", WeightUnit.LB))
        assertTrue(viewModel.uiState.value.bodyweightInvalid)
    }

    @Test
    fun finishingSetsUpTheCoachAndLogsBodyweight() = runTest {
        viewModel.next()
        viewModel.answer(SetupAnswer.Goal("first_pull_up"))
        viewModel.next()
        viewModel.answer(SetupAnswer.PullUp(PullUpAnswer.NOT_YET))
        viewModel.answer(SetupAnswer.Aim(TrainingAim.MUSCLE))
        viewModel.answer(SetupAnswer.Bodyweight("74.5"))
        viewModel.next()
        viewModel.answer(SetupAnswer.Day(DayOfWeek.FRIDAY))
        viewModel.next()
        assertEquals(OnboardingStep.TEST, step)
        viewModel.next(skipTest = true)
        assertEquals(OnboardingStep.READY, step)
        assertNotNull(viewModel.uiState.value.firstSession)

        assertTrue(viewModel.finish())
        val state = checkNotNull(coachRepository.current)
        assertEquals("first_pull_up", state.model.profile.goalPathId)
        assertEquals(TrainingAim.MUSCLE, state.model.profile.aim)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), state.trainingDays)
        assertEquals(74.5, equipment.observeBodyweightKg().first())
    }

    @Test
    fun theQuickTestPlacesEachMovementItCounts() = runTest {
        viewModel.next()
        viewModel.answer(SetupAnswer.Goal(null))
        viewModel.next()
        viewModel.answer(SetupAnswer.PullUp(PullUpAnswer.YES))
        viewModel.next()
        viewModel.answer(SetupAnswer.Kit(Equipment.PULL_UP_BAR))
        viewModel.next()
        assertEquals(OnboardingStep.TEST, step)
        assertEquals(
            listOf(QuickTest.PUSH_UPS, QuickTest.PULL_UPS, QuickTest.PIKE_PUSH_UPS, QuickTest.SQUATS),
            viewModel.uiState.value.tests,
        )

        viewModel.answer(SetupAnswer.TestCount(9))
        assertEquals("horizontal_push.push_up", viewModel.uiState.value.testStart?.first?.id)
        viewModel.next()
        viewModel.answer(SetupAnswer.TestCount(-3))
        assertEquals("never below none", 0, viewModel.uiState.value.testCount)
        assertNull(viewModel.uiState.value.testStart)
        assertTrue(viewModel.back())
        assertEquals("back keeps the count", 9, viewModel.uiState.value.testCount)
        viewModel.next()
        viewModel.next()
        viewModel.next(skipTest = true)
        assertEquals(OnboardingStep.READY, step)
        assertEquals(mapOf(QuickTest.PUSH_UPS to 9, QuickTest.PULL_UPS to 0), viewModel.uiState.value.tested)

        assertTrue(viewModel.finish())
        val model = checkNotNull(coachRepository.current).model
        assertEquals(PUSH_UP_RUNG, model.rung(MovementPattern.HORIZONTAL_PUSH))
        assertEquals("none counted beats the yes", 0, model.rung(MovementPattern.VERTICAL_PULL))
    }

    @Test
    fun backFromThePlanReturnsToTheLastMovement() {
        viewModel.next()
        viewModel.answer(SetupAnswer.Goal(null))
        viewModel.next()
        viewModel.answer(SetupAnswer.PullUp(PullUpAnswer.NOT_YET))
        viewModel.next()
        viewModel.next()
        repeat(viewModel.uiState.value.tests.size) { viewModel.next() }
        assertEquals(OnboardingStep.READY, step)
        viewModel.back()
        assertEquals(OnboardingStep.TEST, step)
        assertEquals(QuickTest.SQUATS, viewModel.uiState.value.test)
    }

    @Test
    fun finishingEarlySavesNothing() = runTest {
        assertFalse(viewModel.finish())
        assertNull(coachRepository.current)
    }
}
