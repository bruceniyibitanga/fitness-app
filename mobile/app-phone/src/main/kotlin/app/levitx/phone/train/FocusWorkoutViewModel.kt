package app.levitx.phone.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.coach.Coach
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.WorkoutFocus
import app.levitx.core.coach.bodyweightTrend
import app.levitx.data.coach.CoachRepository
import app.levitx.data.coach.WorkoutDraftRepository
import app.levitx.data.time.TrainingClock
import app.levitx.phone.handlingDataFailures
import app.levitx.phone.isExpectedDataFailure
import app.levitx.phone.workout.WorkoutStarter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Keep collecting for this long after the screen stops listening, so a rotation doesn't restart the flow. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** What a single workout's screen shows. */
sealed interface FocusWorkoutUiState {
    /** Loading. */
    data object Loading : FocusWorkoutUiState

    /** The data couldn't be read on this device. */
    data object Unavailable : FocusWorkoutUiState

    /** The focus asked for doesn't exist any more (a back stack saved by an older version). */
    data object NotFound : FocusWorkoutUiState

    /**
     * The workout, ready to start.
     *
     * @property focus What it works.
     * @property session The workout, or null when none of it is safe today.
     * @property bodyweightKg Bodyweight for kilograms per rep, or null when none is recorded.
     * @property hasWorkoutInProgress True when another workout is already under way.
     */
    data class Ready(
        val focus: WorkoutFocus,
        val session: PlannedSession?,
        val bodyweightKg: Double?,
        val hasWorkoutInProgress: Boolean,
    ) : FocusWorkoutUiState
}

/**
 * One workout from the Train tab: what it holds today and the button to start it.
 *
 * The workout is worked out by the coach, from the person's own exercises and
 * targets, with anything trained too recently eased.
 */
@HiltViewModel(assistedFactory = FocusWorkoutViewModel.Factory::class)
class FocusWorkoutViewModel @AssistedInject constructor(
    @Assisted focusName: String,
    coachRepository: CoachRepository,
    drafts: WorkoutDraftRepository,
    private val coach: Coach,
    private val clock: TrainingClock,
    private val starter: WorkoutStarter,
) : ViewModel() {
    private val focus: WorkoutFocus? = WorkoutFocus.entries.firstOrNull { it.name == focusName }

    /** The current screen state. */
    val uiState: StateFlow<FocusWorkoutUiState> =
        combine(coachRepository.observeState(), drafts.observeDraft()) { state, draft ->
            when {
                focus == null -> FocusWorkoutUiState.NotFound

                state == null -> FocusWorkoutUiState.Loading

                else -> {
                    val now = clock.currentTimeMillis()
                    FocusWorkoutUiState.Ready(
                        focus = focus,
                        session = coach.workout(state.model, focus, now),
                        bodyweightKg = bodyweightTrend(state.model.weighIns, now)?.currentKg,
                        hasWorkoutInProgress = draft != null,
                    )
                }
            }
        }
            .catch { error ->
                if (error.isExpectedDataFailure()) emit(FocusWorkoutUiState.Unavailable) else throw error
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FocusWorkoutUiState.Loading)

    /**
     * Starts this workout, or keeps the one already in progress.
     *
     * @return True when there is a workout to open.
     */
    suspend fun start(): Boolean {
        val session = (uiState.value as? FocusWorkoutUiState.Ready)?.session ?: return false
        return handlingDataFailures(onFailure = { false }) {
            starter.start(session)
            true
        }
    }

    /** Makes a [FocusWorkoutViewModel] for one focus, named as in [WorkoutFocus]. */
    @AssistedFactory
    interface Factory {
        /** The ViewModel for the focus called [focusName]. */
        fun create(focusName: String): FocusWorkoutViewModel
    }
}
