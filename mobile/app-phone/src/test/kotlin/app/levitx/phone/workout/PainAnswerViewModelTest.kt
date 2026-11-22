package app.levitx.phone.workout

import app.levitx.core.coach.BodyArea
import app.levitx.core.pain.PainAdvice
import app.levitx.core.pain.PainAssessment
import app.levitx.core.pain.PainReason
import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeWorkoutDraftRepository
import app.levitx.testing.coachState
import app.levitx.testing.newcomer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// UnconfinedTestDispatcher is still marked experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class PainAnswerViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule(UnconfinedTestDispatcher())

    private val easeOff = PainAssessment(PainAdvice.EASE_OFF, PainReason.TOO_HIGH_DURING_EXERCISE)
    private val sore = newcomer().withPain(BodyArea.ELBOW, easeOff).withPain(BodyArea.KNEE, easeOff)
    private val coachRepository = FakeCoachRepository(coachState(sore), FakeWorkoutDraftRepository())

    @Test
    fun fineTakesOnlyThatAreaOffLightWork() = runTest {
        PainAnswerViewModel(coachRepository).settled(BodyArea.ELBOW)
        assertEquals(mapOf(BodyArea.KNEE to PainAdvice.EASE_OFF), coachRepository.current?.model?.pain)
    }

    @Test
    fun fineForAnAreaThatWasNotSoreChangesNothing() = runTest {
        PainAnswerViewModel(coachRepository).settled(BodyArea.WRIST)
        assertEquals(sore.pain, coachRepository.current?.model?.pain)
    }
}
