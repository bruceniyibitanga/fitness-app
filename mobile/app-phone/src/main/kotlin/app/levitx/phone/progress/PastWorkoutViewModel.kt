package app.levitx.phone.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.history.WorkoutRecord
import app.levitx.data.coach.CoachRepository
import app.levitx.data.history.HistoryRepository
import app.levitx.phone.handlingDataFailures
import app.levitx.phone.isExpectedDataFailure
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
     * @property deleting True while it is being deleted.
     */
    data class Ready(val workout: WorkoutRecord, val deleting: Boolean = false) : PastWorkoutUiState

    /** Deleted. Time to go back. */
    data object Deleted : PastWorkoutUiState
}

/** One past workout: every exercise and set, as it was logged, and deleting it. */
@HiltViewModel(assistedFactory = PastWorkoutViewModel.Factory::class)
class PastWorkoutViewModel @AssistedInject constructor(
    @Assisted private val workoutId: String,
    history: HistoryRepository,
    private val coachRepository: CoachRepository,
) : ViewModel() {
    private val deletion = MutableStateFlow(Deletion.NONE)

    /** The current screen state. */
    val uiState: StateFlow<PastWorkoutUiState> = combine(history.observeWorkouts(), deletion) { workouts, deletion ->
        val workout = workouts.firstOrNull { it.id == workoutId }
        when {
            deletion == Deletion.DONE -> PastWorkoutUiState.Deleted
            workout == null -> PastWorkoutUiState.NotFound
            else -> PastWorkoutUiState.Ready(workout, deleting = deletion == Deletion.RUNNING)
        }
    }
        .catch { error -> if (error.isExpectedDataFailure()) emit(PastWorkoutUiState.Unavailable) else throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PastWorkoutUiState.Loading)

    /**
     * Deletes the workout, after the person confirmed it, and takes it back
     * out of what the coach knows. Once.
     */
    fun delete() {
        if (deletion.value != Deletion.NONE) return
        deletion.value = Deletion.RUNNING
        viewModelScope.launch {
            deletion.value = handlingDataFailures(onFailure = { Deletion.NONE }) {
                coachRepository.deleteWorkout(workoutId)
                Deletion.DONE
            }
        }
    }

    /** Makes one for a workout. */
    @AssistedFactory
    interface Factory {
        /** The view model for [workoutId]. */
        fun create(workoutId: String): PastWorkoutViewModel
    }
}

/** How far deleting has got. */
private enum class Deletion { NONE, RUNNING, DONE }
