package app.levitx.phone.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.coach.Coach
import app.levitx.core.coach.SuggestionChoice
import app.levitx.core.coach.bodyweightTrend
import app.levitx.data.coach.CoachRepository
import app.levitx.data.coach.WorkoutDraftRepository
import app.levitx.data.time.TrainingClock
import app.levitx.phone.handlingDataFailures
import app.levitx.phone.isExpectedDataFailure
import app.levitx.phone.workout.WorkoutStarter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Keep collecting for this long after the screen stops listening, so a
 * rotation doesn't restart the flow. Coming back from the background after
 * longer restarts it, which works the plan out again for the new time.
 */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Today: asks the coach for the next session and starts it.
 *
 * Starting goes through [WorkoutStarter], which saves the workout as the
 * draft before the workout screen opens.
 */
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val coachRepository: CoachRepository,
    drafts: WorkoutDraftRepository,
    private val coach: Coach,
    private val clock: TrainingClock,
    private val starter: WorkoutStarter,
) : ViewModel() {
    /** The current screen state. */
    val uiState: StateFlow<TodayUiState> =
        combine(coachRepository.observeState(), drafts.observeDraft()) { state, draft ->
            if (state == null) {
                TodayUiState.Loading
            } else {
                val now = clock.currentTimeMillis()
                TodayUiState.Ready(
                    nowEpochMillis = now,
                    recommendation = coach.recommend(state.model, now),
                    bodyweightKg = bodyweightTrend(state.model.weighIns, now)?.currentKg,
                    hasWorkoutInProgress = draft != null,
                )
            }
        }
            .catch { error -> if (error.isExpectedDataFailure()) emit(TodayUiState.Unavailable) else throw error }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TodayUiState.Loading)

    /**
     * Starts the session, the lighter version when [choice] is
     * [SuggestionChoice.LIGHTER], unless a workout is already in progress, which
     * is resumed instead. [choice] is null when the coach offered nothing.
     *
     * @return True when there is a workout to open.
     */
    suspend fun start(choice: SuggestionChoice? = null): Boolean {
        val ready = uiState.value as? TodayUiState.Ready
        val session = ready?.recommendation?.let { recommendation ->
            if (choice == SuggestionChoice.LIGHTER) recommendation.suggestion?.lighter else recommendation.session
        }
        return when {
            ready == null -> false

            ready.hasWorkoutInProgress -> true

            session == null -> false

            else -> handlingDataFailures(onFailure = { false }) {
                starter.start(session, choice)
                true
            }
        }
    }

    /** Takes the rest day the coach offered. The session waits for next time. */
    fun rest() {
        viewModelScope.launch {
            handlingDataFailures(onFailure = {}) { coachRepository.recordRest(clock.currentTimeMillis()) }
        }
    }

    /** Marks the fast weight loss notice as read, so it isn't shown again this spell. */
    fun acknowledgeWeightNotice() {
        viewModelScope.launch {
            handlingDataFailures(onFailure = {}) { coachRepository.markWeightNoticeShown() }
        }
    }
}
