package app.levitx.wear.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.session.TrainingDay
import app.levitx.core.session.trainingDay
import app.levitx.data.DataUnavailableException
import app.levitx.data.goal.GoalRepository
import app.levitx.data.progress.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Keep collecting for this long after the screen stops listening, so a
 * brief interruption (like ambient mode) doesn't restart the flow.
 */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Turns the active goal and the person's bests into [HomeUiState] for the watch.
 *
 * In Phase 3 this screen becomes the start of a session, and the goal and
 * bests arrive from the phone over the Data Layer. Until then the watch reads
 * its own (empty) database and asks the person to choose a goal on the phone.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    goalRepository: GoalRepository,
    progressRepository: ProgressRepository,
) : ViewModel() {
    /** The current screen state. Starts as [HomeUiState.Loading]. */
    val uiState: StateFlow<HomeUiState> =
        combine(goalRepository.observeActiveGoal(), progressRepository.observeBests()) { path, bests ->
            path?.trainingDay(bests)?.toUiState() ?: HomeUiState.NoGoal
        }
            // Only the expected failure becomes a screen state. Anything else is
            // a bug and should crash loudly rather than hide behind an error message.
            .catch { error -> if (error is DataUnavailableException) emit(HomeUiState.Unavailable) else throw error }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = HomeUiState.Loading,
            )
}

private fun TrainingDay.toUiState() = HomeUiState.Ready(
    skillName = path.name,
    gateTitle = currentGate?.title,
    gateNumber = gateNumber,
    gateCount = path.gates.size,
)
