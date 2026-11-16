package app.levitx.phone.onboarding

import androidx.lifecycle.ViewModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.PullUpAnswer
import app.levitx.data.coach.CoachRepository
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.time.TrainingClock
import app.levitx.phone.handlingDataFailures
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Setup. Collects the few answers the coach can't work out for itself, shows
 * the first session, then saves everything in one go.
 *
 * Only what can't be learned from training is asked: the goal, the week and
 * the kit. Level is measured in the first sessions, or sooner with the
 * optional quick test, which counts one set of a few movements.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val coachRepository: CoachRepository,
    private val equipmentRepository: EquipmentRepository,
    private val coach: Coach,
    private val clock: TrainingClock,
) : ViewModel() {
    private val state = MutableStateFlow(OnboardingUiState())

    /** The current screen state. */
    val uiState: StateFlow<OnboardingUiState> = state.asStateFlow()

    /**
     * Moves to the next step, if this one has what it needs. In the quick test
     * it confirms the count on screen and moves to the next movement first,
     * or with [skipTest], skips the rest of the test and keeps what was confirmed.
     */
    fun next(skipTest: Boolean = false) {
        val now = clock.currentTimeMillis()
        state.update { current ->
            val moved = if (skipTest) current.skippingTest() else current.forward()
            val arrived = moved.step == OnboardingStep.READY && current.step != OnboardingStep.READY
            if (arrived) moved.copy(firstSession = coach.firstSession(moved, now)) else moved
        }
    }

    /**
     * Goes back a step, or a movement within the quick test.
     *
     * @return False on the first step, where back should leave the app.
     */
    fun back(): Boolean {
        val current = state.value
        if (current.step == OnboardingStep.WELCOME || current.saving) return false
        state.update { it.backward() }
        return true
    }

    /** Takes one setup answer: a choice, a field typed, or a quick test count. */
    fun answer(answer: SetupAnswer) = state.update { it.with(answer) }

    /**
     * Saves the answers and sets up the coach. Bodyweight, when given, goes to
     * the bodyweight log like any other reading.
     *
     * @return True when saved, so the caller can move on.
     */
    suspend fun finish(): Boolean {
        val answers = state.value
        if (answers.saving || answers.step != OnboardingStep.READY) return false
        state.update { it.copy(saving = true) }
        return handlingDataFailures(
            onFailure = {
                state.update { it.copy(saving = false, unavailable = true) }
                false
            },
        ) {
            answers.bodyweightKg?.let { equipmentRepository.recordBodyweight(it) }
            coachRepository.start(
                answers.profile,
                answers.pullUp ?: PullUpAnswer.NOT_SURE,
                answers.days,
                answers.tested,
            )
            true
        }
    }
}

/** The first session the coach would plan from [answers], to show before anything is saved. */
private fun Coach.firstSession(answers: OnboardingUiState, nowEpochMillis: Long): PlannedSession? =
    recommend(answers.startingModel, nowEpochMillis).session
