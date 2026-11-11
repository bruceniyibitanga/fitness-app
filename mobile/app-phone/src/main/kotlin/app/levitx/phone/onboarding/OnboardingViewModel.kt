package app.levitx.phone.onboarding

import androidx.lifecycle.ViewModel
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PlanStyle
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.PullUpAnswer
import app.levitx.data.coach.CoachRepository
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.time.TrainingClock
import app.levitx.phone.handlingDataFailures
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
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
 * the kit. Level is measured in the first sessions, not asked.
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

    /** Moves to the next step, if this one has what it needs. */
    fun next() {
        val current = state.value
        if (!current.canContinue || current.step == OnboardingStep.READY) return
        val nextStep = OnboardingStep.entries[current.step.ordinal + 1]
        val now = clock.currentTimeMillis()
        state.update {
            it.copy(
                step = nextStep,
                firstSession = if (nextStep == OnboardingStep.READY) coach.firstSession(it, now) else it.firstSession,
            )
        }
    }

    /**
     * Goes back a step.
     *
     * @return False on the first step, where back should leave the app.
     */
    fun back(): Boolean {
        val current = state.value
        if (current.step == OnboardingStep.WELCOME || current.saving) return false
        state.update { it.copy(step = OnboardingStep.entries[it.step.ordinal - 1]) }
        return true
    }

    /** Picks a skill path by id, or general fitness with null. */
    fun chooseGoal(pathId: String?) = state.update { it.copy(goalChosen = true, goalPathId = pathId) }

    /** Picks all-round or focused. */
    fun chooseStyle(style: PlanStyle) = state.update { it.copy(style = style) }

    /** Answers "Can you do a strict pull-up?". */
    fun answerPullUp(answer: PullUpAnswer) = state.update { it.copy(pullUp = answer) }

    /** Keeps what is typed in the bodyweight field. Short, so nothing silly can be stored. */
    fun typeBodyweight(text: String) = state.update { it.copy(bodyweightText = text.take(MAX_WEIGHT_CHARS)) }

    /** Adds or removes a training day. */
    fun toggleDay(day: DayOfWeek) = state.update { it.copy(days = it.days.toggled(day)) }

    /** Picks the time per session. */
    fun chooseMinutes(minutes: Int) = state.update { it.copy(minutes = minutes) }

    /** Adds or removes a piece of kit. */
    fun toggleEquipment(equipment: Equipment) = state.update { it.copy(equipment = it.equipment.toggled(equipment)) }

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
            coachRepository.start(answers.profile, answers.pullUp ?: PullUpAnswer.NOT_SURE, answers.days)
            true
        }
    }

    private companion object {
        const val MAX_WEIGHT_CHARS = 6
    }
}

/** The first session the coach would plan from [answers], to show before anything is saved. */
private fun Coach.firstSession(answers: OnboardingUiState, nowEpochMillis: Long): PlannedSession? = recommend(
    AthleteModel.starting(answers.profile, answers.pullUp ?: PullUpAnswer.NOT_SURE),
    nowEpochMillis,
).session

private fun <T> Set<T>.toggled(item: T): Set<T> = if (item in this) this - item else this + item
