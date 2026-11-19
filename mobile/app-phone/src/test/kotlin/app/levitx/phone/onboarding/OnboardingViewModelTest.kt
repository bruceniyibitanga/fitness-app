package app.levitx.phone.onboarding

import app.levitx.core.coach.Coach
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.MovementPattern
import app.levitx.core.coach.PlanStyle
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.QuickTest
import app.levitx.core.coach.TrainingAim
import app.levitx.core.load.WeightUnit
import app.levitx.data.settings.AppSettings
import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeEquipmentRepository
import app.levitx.testing.FakeSettingsRepository
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
    private val settings = FakeSettingsRepository()

    // Lazy, so it is built after the rule has swapped in the test main dispatcher.
    private val viewModel by lazy {
        OnboardingViewModel(coachRepository, equipment, Coach(), FakeTrainingClock(), settings)
    }

    private val step: OnboardingStep
        get() = viewModel.uiState.value.step

    /** Moves setup on from the step on screen, giving it the plainest answer it accepts. */
    private fun advance(goal: String? = null) {
        when (step) {
            OnboardingStep.GOAL -> {
                viewModel.answer(SetupAnswer.Goal(goal))
                viewModel.next()
            }

            OnboardingStep.STYLE -> viewModel.answer(SetupAnswer.Style(PlanStyle.ALL_ROUND))

            OnboardingStep.AIM -> viewModel.answer(SetupAnswer.Aim(TrainingAim.BALANCED))

            OnboardingStep.PULL_UP -> viewModel.answer(SetupAnswer.PullUp(PullUpAnswer.NOT_YET))

            OnboardingStep.BODY, OnboardingStep.TEST -> viewModel.next(skip = true)

            else -> viewModel.next()
        }
    }

    /** Answers setup as far as [target]. */
    private fun goTo(target: OnboardingStep, goal: String? = null) {
        while (step != target) advance(goal)
    }

    @Test
    fun aStepWaitsForWhatItNeeds() {
        viewModel.next()
        assertEquals(OnboardingStep.GOAL, step)
        viewModel.next()
        assertEquals("no goal picked yet", OnboardingStep.GOAL, step)

        goTo(OnboardingStep.PULL_UP)
        viewModel.next()
        assertEquals("the pull-up question needs an answer", OnboardingStep.PULL_UP, step)

        goTo(OnboardingStep.BODY)
        viewModel.answer(SetupAnswer.Bodyweight("abc"))
        viewModel.next()
        assertEquals("a weight that isn't one", OnboardingStep.BODY, step)

        goTo(OnboardingStep.WEEK)
        DayOfWeek.entries.forEach { if (it in viewModel.uiState.value.days) viewModel.answer(SetupAnswer.Day(it)) }
        viewModel.next()
        assertEquals("no training days", OnboardingStep.WEEK, step)
    }

    @Test
    fun everyStepAsksOneThingInOrderAndPlanStyleIsOnlyAskedWithASkillGoal() {
        val seen = mutableListOf(step)
        while (step != OnboardingStep.READY) {
            advance(goal = "first_pull_up")
            if (seen.last() != step) seen += step
        }
        assertEquals(OnboardingStep.entries, seen)

        val general = OnboardingUiState(goalChosen = true, goalPathId = null)
        assertFalse(OnboardingStep.STYLE in general.steps)
        assertEquals(OnboardingStep.AIM, general.copy(step = OnboardingStep.GOAL).forward().step)
        assertEquals(OnboardingStep.GOAL, general.copy(step = OnboardingStep.AIM).backward().step)
    }

    @Test
    fun anAnswerThatIsTheWholeStepMovesOnAndBackShowsItStillPicked() {
        goTo(OnboardingStep.STYLE, goal = "first_pull_up")

        viewModel.answer(SetupAnswer.Style(PlanStyle.FOCUSED))
        assertEquals(OnboardingStep.AIM, step)
        viewModel.answer(SetupAnswer.Aim(TrainingAim.STRENGTH))
        assertEquals(OnboardingStep.PULL_UP, step)
        viewModel.answer(SetupAnswer.PullUp(PullUpAnswer.YES))
        assertEquals(OnboardingStep.BODY, step)

        viewModel.back()
        viewModel.back()
        assertEquals(OnboardingStep.AIM, step)
        assertEquals(TrainingAim.STRENGTH, viewModel.uiState.value.aim)
        assertEquals(PlanStyle.FOCUSED, viewModel.uiState.value.style)
        assertEquals(PullUpAnswer.YES, viewModel.uiState.value.pullUp)
    }

    @Test
    fun stepsWithSeveralAnswersWaitForContinue() {
        goTo(OnboardingStep.WEEK)
        viewModel.answer(SetupAnswer.Day(DayOfWeek.SATURDAY))
        viewModel.answer(SetupAnswer.Minutes(30))
        assertEquals(OnboardingStep.WEEK, step)

        viewModel.next()
        viewModel.answer(SetupAnswer.Kit(Equipment.PULL_UP_BAR))
        assertEquals(OnboardingStep.KIT, step)
        // The aim can't move setup on from a step that isn't asking for it.
        viewModel.answer(SetupAnswer.Aim(TrainingAim.MUSCLE))
        assertEquals(OnboardingStep.KIT, step)
    }

    @Test
    fun progressRunsFromTheWelcomeToThePlan() {
        assertEquals(0f, viewModel.uiState.value.progress)
        var last = 0f
        while (step != OnboardingStep.READY) {
            advance()
            assertTrue("never goes backwards", viewModel.uiState.value.progress >= last)
            last = viewModel.uiState.value.progress
        }
        assertEquals(1f, last)
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
    fun theUnitStartsAsTheAppsAndCanBeSwitchedDuringSetup() {
        settings.settings.value = AppSettings(units = WeightUnit.LB)
        viewModel.answer(SetupAnswer.Bodyweight("165"))
        assertEquals(WeightUnit.LB, viewModel.uiState.value.bodyweightUnit)
        assertEquals(74.84, checkNotNull(viewModel.uiState.value.bodyweightKg), 0.01)

        // Someone who reads kilograms typed a number under "lb" that is out of range in pounds.
        viewModel.answer(SetupAnswer.Bodyweight("30"))
        assertTrue(viewModel.uiState.value.bodyweightInvalid)

        viewModel.chooseUnit(WeightUnit.KG)

        // The number typed is kept and read in the unit picked, and the whole app follows.
        assertEquals("30", viewModel.uiState.value.bodyweightText)
        assertEquals(30.0, checkNotNull(viewModel.uiState.value.bodyweightKg), 0.01)
        assertEquals(WeightUnit.KG, settings.settings.value.units)
    }

    @Test
    fun skippingBodyweightDropsWhatWasTypedEvenIfItWasWrong() = runTest {
        goTo(OnboardingStep.BODY)
        viewModel.answer(SetupAnswer.Bodyweight("abc"))

        viewModel.next(skip = true)

        assertEquals(OnboardingStep.WEEK, step)
        assertNull(viewModel.uiState.value.bodyweightKg)
        goTo(OnboardingStep.READY)
        assertTrue(viewModel.finish())
        assertNull(equipment.observeBodyweightKg().first())
    }

    @Test
    fun finishingSetsUpTheCoachAndLogsBodyweight() = runTest {
        goTo(OnboardingStep.AIM, goal = "first_pull_up")
        viewModel.answer(SetupAnswer.Aim(TrainingAim.MUSCLE))
        goTo(OnboardingStep.BODY)
        viewModel.answer(SetupAnswer.Bodyweight("74.5"))
        viewModel.next()
        viewModel.answer(SetupAnswer.Day(DayOfWeek.FRIDAY))
        goTo(OnboardingStep.TEST)
        viewModel.next(skip = true)
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
        goTo(OnboardingStep.PULL_UP)
        viewModel.answer(SetupAnswer.PullUp(PullUpAnswer.YES))
        goTo(OnboardingStep.KIT)
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
        viewModel.next(skip = true)
        assertEquals(OnboardingStep.READY, step)
        assertEquals(mapOf(QuickTest.PUSH_UPS to 9, QuickTest.PULL_UPS to 0), viewModel.uiState.value.tested)

        assertTrue(viewModel.finish())
        val model = checkNotNull(coachRepository.current).model
        assertEquals(PUSH_UP_RUNG, model.rung(MovementPattern.HORIZONTAL_PUSH))
        assertEquals("none counted beats the yes", 0, model.rung(MovementPattern.VERTICAL_PULL))
    }

    @Test
    fun backFromThePlanReturnsToTheLastMovement() {
        goTo(OnboardingStep.TEST)
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
