package app.levitx.phone.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.data.coach.CoachRepository
import app.levitx.data.time.TrainingClock
import app.levitx.phone.isExpectedDataFailure
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Keep collecting for this long after the screen stops listening, so a rotation doesn't restart the flow. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** One exercise: how to do it, the person's numbers on it, and its ladder. Read-only. */
@HiltViewModel(assistedFactory = ExerciseViewModel.Factory::class)
class ExerciseViewModel @AssistedInject constructor(
    @Assisted exerciseId: String,
    coachRepository: CoachRepository,
    clock: TrainingClock,
) : ViewModel() {
    /** The current screen state. */
    val uiState: StateFlow<ExerciseUiState> = coachRepository.observeState()
        .map { state ->
            state?.let { exerciseState(it.model, exerciseId, clock.currentTimeMillis()) } ?: ExerciseUiState.Loading
        }
        .catch { error -> if (error.isExpectedDataFailure()) emit(ExerciseUiState.Unavailable) else throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ExerciseUiState.Loading)

    /** Makes an [ExerciseViewModel] for one exercise. */
    @AssistedFactory
    interface Factory {
        /** The ViewModel for the exercise with [exerciseId]. */
        fun create(exerciseId: String): ExerciseViewModel
    }
}
