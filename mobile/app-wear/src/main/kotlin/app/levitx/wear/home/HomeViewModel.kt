package app.levitx.wear.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.coach.PlannedSession
import app.levitx.core.session.SetLoad
import app.levitx.data.DataUnavailableException
import app.levitx.data.coach.NextSessionRepository
import app.levitx.data.coach.WorkoutOutbox
import app.levitx.data.equipment.Band
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.goal.GoalRepository
import app.levitx.wear.session.WorkoutController
import app.levitx.wear.session.WorkoutState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Keep collecting for this long after the screen stops listening, so a
 * brief interruption (like ambient mode) doesn't restart the flow.
 */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Turns the coach's next session, the goal and the workout in progress into
 * [HomeUiState].
 *
 * The next session, goal and bands are copies of the phone's, kept in the
 * watch's own database by sync, so this screen works with the phone out of
 * reach.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    goalRepository: GoalRepository,
    nextSessionRepository: NextSessionRepository,
    equipmentRepository: EquipmentRepository,
    workoutOutbox: WorkoutOutbox,
    private val controller: WorkoutController,
) : ViewModel() {
    // Typed on purpose: the branches below are different HomeUiState types,
    // and without the type the compiler can't tell what `catch` may emit.
    private val readyState: Flow<HomeUiState> = combine(
        goalRepository.observeActiveGoal(),
        nextSessionRepository.observeNext(),
        controller.state,
        equipmentRepository.observeBands(),
        workoutOutbox.observeQueued(),
    ) { path, next, workout, bands, queued ->
        if (workout is WorkoutState.Unavailable) {
            HomeUiState.Unavailable
        } else {
            HomeUiState.Ready(
                goalName = path?.name,
                next = next?.summary(),
                isSessionRunning = workout is WorkoutState.Running,
                load = (workout as? WorkoutState.Idle)?.let { idle -> next?.loadSummary(idle.load, bands) },
                waitingToSync = queued.size,
            )
        }
    }

    /** The current screen state. Starts as [HomeUiState.Loading]. */
    val uiState: StateFlow<HomeUiState> = readyState
        // Only the expected failure becomes a screen state. Anything else is
        // a bug and should crash loudly rather than hide behind an error message.
        .catch { error -> if (error is DataUnavailableException) emit(HomeUiState.Unavailable) else throw error }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState.Loading,
        )

    /** Starts the next session. The workout screen shows it as soon as it is set up. */
    fun startSession() = controller.start()
}

/**
 * The load to show for this session, or null when its skill work has no load
 * to track (a hold, say) or it has no skill work. Matches what
 * [WorkoutController] stamps on the sets.
 */
private fun PlannedSession.loadSummary(load: SetLoad, bands: List<Band>): LoadSummary? {
    val tracksLoad = plans.any { it.gateId != null && it.exercise.loadProfile != null }
    if (!tracksLoad) return null
    val band = bands.firstOrNull { load.assistanceKg > 0.0 && it.assistanceKg == load.assistanceKg }
    return LoadSummary(addedKg = load.addedKg, bandName = band?.name)
}
