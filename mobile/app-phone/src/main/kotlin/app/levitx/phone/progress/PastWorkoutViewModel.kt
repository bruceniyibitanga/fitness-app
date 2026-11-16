package app.levitx.phone.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.history.WorkoutRecord
import app.levitx.data.history.HistoryRepository
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

/** Keep collecting through a rotation. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** One past workout. */
sealed interface PastWorkoutUiState {
    /** Reading the history. */
    data object Loading : PastWorkoutUiState

    /** The data can't be read on this device right now. */
    data object Unavailable : PastWorkoutUiState

    /** No workout with that id. */
    data object NotFound : PastWorkoutUiState

    /**
     * Ready to show.
     *
     * @property workout The workout, with every set.
     */
    data class Ready(val workout: WorkoutRecord) : PastWorkoutUiState
}

/** One past workout: every exercise and set, as it was logged. */
@HiltViewModel(assistedFactory = PastWorkoutViewModel.Factory::class)
class PastWorkoutViewModel @AssistedInject constructor(@Assisted workoutId: String, history: HistoryRepository) :
    ViewModel() {
    /** The current screen state. */
    val uiState: StateFlow<PastWorkoutUiState> = history.observeWorkouts()
        .map { workouts ->
            workouts.firstOrNull { it.id == workoutId }?.let(PastWorkoutUiState::Ready) ?: PastWorkoutUiState.NotFound
        }
        .catch { error -> if (error.isExpectedDataFailure()) emit(PastWorkoutUiState.Unavailable) else throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PastWorkoutUiState.Loading)

    /** Makes one for a workout. */
    @AssistedFactory
    interface Factory {
        /** The view model for [workoutId]. */
        fun create(workoutId: String): PastWorkoutViewModel
    }
}
