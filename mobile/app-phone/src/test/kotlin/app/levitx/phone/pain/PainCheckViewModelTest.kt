package app.levitx.phone.pain

import app.levitx.core.coach.BodyArea
import app.levitx.core.pain.PainAdvice
import app.levitx.core.pain.PainAssessment
import app.levitx.core.pain.PainReason
import app.levitx.core.pain.RedFlag
import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeWorkoutDraftRepository
import app.levitx.testing.coachState
import app.levitx.testing.newcomer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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
class PainCheckViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule(UnconfinedTestDispatcher())

    private val coachRepository = FakeCoachRepository(coachState(), FakeWorkoutDraftRepository())
    private val viewModel by lazy { PainCheckViewModel(coachRepository) }

    private val pain: Map<BodyArea, PainAdvice>?
        get() = coachRepository.current?.model?.pain

    private fun TestScope.ready(): PainCheckUiState.Ready {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel.uiState.value as PainCheckUiState.Ready
    }

    private val now: PainCheckUiState.Ready
        get() = viewModel.uiState.value as PainCheckUiState.Ready

    @Test
    fun startsByAskingWhere() = runTest {
        val state = ready()
        assertEquals(PainStep.AREA, state.step)
        assertNull(state.area)
        assertFalse(state.canContinue)
    }

    @Test
    fun aRatingOverTheGuidePutsTheAreaOnLightWork() = runTest {
        ready()
        viewModel.chooseArea(BodyArea.ELBOW)
        viewModel.next()
        assertEquals(PainStep.RATING, now.step)
        assertFalse("nothing is picked for the person", now.canContinue)
        viewModel.chooseRating(7)
        viewModel.next()
        assertEquals(mapOf(BodyArea.ELBOW to PainAdvice.EASE_OFF), pain)
        assertTrue(now.saved)
    }

    @Test
    fun aRatingInsideTheGuideChangesNothing() = runTest {
        ready()
        viewModel.chooseArea(BodyArea.KNEE)
        viewModel.next()
        viewModel.chooseRating(4)
        viewModel.next()
        assertEquals(emptyMap<BodyArea, PainAdvice>(), pain)
        assertTrue(now.saved)
    }

    @Test
    fun aWarningSignPausesTheAreaWithoutAskingForARating() = runTest {
        ready()
        viewModel.chooseArea(BodyArea.SHOULDER)
        viewModel.toggleSign(RedFlag.NUMBNESS_OR_TINGLING)
        viewModel.next()
        assertEquals(mapOf(BodyArea.SHOULDER to PainAdvice.SEE_PROFESSIONAL), pain)
        assertTrue(now.saved)
    }

    @Test
    fun aSignSwitchedOffAgainNoLongerPauses() = runTest {
        ready()
        viewModel.chooseArea(BodyArea.SHOULDER)
        viewModel.toggleSign(RedFlag.SUDDEN_INJURY)
        viewModel.toggleSign(RedFlag.SUDDEN_INJURY)
        viewModel.next()
        assertEquals(PainStep.RATING, now.step)
        assertEquals(emptyMap<BodyArea, PainAdvice>(), pain)
    }

    @Test
    fun checkingASoreAreaAgainWithNoPainPutsItBackToNormal() = runTest {
        val sore = newcomer().withPain(
            BodyArea.ELBOW,
            PainAssessment(PainAdvice.SEE_PROFESSIONAL, PainReason.RED_FLAG),
        )
        val repository = FakeCoachRepository(coachState(sore), FakeWorkoutDraftRepository())
        val check = PainCheckViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { check.uiState.collect {} }
        val shown = check.uiState.value as PainCheckUiState.Ready
        assertEquals(PainAdvice.SEE_PROFESSIONAL, shown.sore[BodyArea.ELBOW])
        check.chooseArea(BodyArea.ELBOW)
        check.next()
        check.chooseRating(0)
        check.next()
        assertEquals(emptyMap<BodyArea, PainAdvice>(), repository.current?.model?.pain)
    }

    @Test
    fun backStepsThroughTheCheckAndForgetsTheAnswers() = runTest {
        ready()
        assertFalse("back on the first step leaves the screen", viewModel.back())
        viewModel.chooseArea(BodyArea.WRIST)
        viewModel.next()
        viewModel.chooseRating(8)
        assertTrue(viewModel.back())
        assertEquals(PainStep.SIGNS, now.step)
        assertTrue(viewModel.back())
        assertEquals(PainStep.AREA, now.step)
        assertNull(now.area)
        assertNull(now.rating)
        assertEquals(emptyMap<BodyArea, PainAdvice>(), pain)
    }

    @Test
    fun pickingAnotherAreaStartsTheAnswersAfresh() = runTest {
        ready()
        viewModel.chooseArea(BodyArea.WRIST)
        viewModel.toggleSign(RedFlag.SUDDEN_INJURY)
        viewModel.back()
        viewModel.chooseArea(BodyArea.HIP)
        assertEquals(BodyArea.HIP, now.area)
        assertTrue(now.signs.isEmpty())
    }

    @Test
    fun aNumberOffTheScaleIsIgnored() = runTest {
        ready()
        viewModel.chooseArea(BodyArea.HIP)
        viewModel.next()
        viewModel.chooseRating(11)
        viewModel.chooseRating(-1)
        assertNull(now.rating)
        viewModel.next()
        assertFalse(now.saved)
    }

    @Test
    fun aSecondTapDoesNotStoreTheAnswerTwiceOrMoveOn() = runTest {
        ready()
        viewModel.chooseArea(BodyArea.ANKLE)
        viewModel.next()
        viewModel.chooseRating(9)
        viewModel.next()
        viewModel.next()
        viewModel.chooseArea(BodyArea.WRIST)
        assertEquals(mapOf(BodyArea.ANKLE to PainAdvice.EASE_OFF), pain)
        assertEquals(BodyArea.ANKLE, now.area)
    }

    @Test
    fun beforeSetupThereIsNothingToCheck() = runTest {
        val empty = PainCheckViewModel(FakeCoachRepository(null, FakeWorkoutDraftRepository()))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { empty.uiState.collect {} }
        assertEquals(PainCheckUiState.Loading, empty.uiState.value)
    }
}
