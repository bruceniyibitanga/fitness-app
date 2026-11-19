package app.levitx.phone.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.coach.Coach
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.load.WeightUnit
import app.levitx.data.coach.CoachRepository
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.settings.SettingsRepository
import app.levitx.data.time.TrainingClock
import app.levitx.phone.handlingDataFailures
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val settings: SettingsRepository,
) : ViewModel() {
    private val state = MutableStateFlow(OnboardingUiState())

    init {
        // Setup types and shows weights in the app's unit, and follows it if it changes.
        viewModelScope.launch {
            settings.observe().map { it.units }.distinctUntilChanged()
                // A settings file that can't be read leaves the default unit; the switch on the step still works.
                .catch { error -> if (error !is IOException) throw error }
                .collect { unit -> state.update { it.with(SetupAnswer.Unit(unit)) } }
        }
    }

    /** The current screen state. */
    val uiState: StateFlow<OnboardingUiState> = state.asStateFlow()

    /**
     * Moves to the next step, if this one has what it needs. In the quick test
     * it confirms the count on screen and moves to the next movement first.
     * With [skip], it passes a step that may be skipped: bodyweight, or the
     * rest of the quick test (what was confirmed is kept).
     */
    fun next(skip: Boolean = false) = move { if (skip) it.skipping() else it.forward() }

    /** Applies [change], and works out the first session on arriving at the plan. */
    private fun move(change: (OnboardingUiState) -> OnboardingUiState) {
        val now = clock.currentTimeMillis()
        state.update { current ->
            val moved = change(current)
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

    /**
     * Takes one setup answer: a choice, a field typed, or a quick test count.
     * When the answer is all its step asks (plan style, what matters most,
     * the pull-up), setup moves on with it.
     */
    fun answer(answer: SetupAnswer) = move { current ->
        val taken = current.with(answer)
        if (current.answers(answer)) taken.forward() else taken
    }

    /**
     * Switches between kilograms and pounds, for setup and the rest of the
     * app. It is the same setting as the one in Settings, saved at once so
     * every weight on the steps that follow is in the unit just picked.
     */
    fun chooseUnit(unit: WeightUnit) {
        state.update { it.with(SetupAnswer.Unit(unit)) }
        viewModelScope.launch {
            // If the setting can't be saved, setup still uses the unit picked for the weight typed here.
            try {
                settings.setUnits(unit)
            } catch (_: IOException) {
                Unit
            }
        }
    }

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
