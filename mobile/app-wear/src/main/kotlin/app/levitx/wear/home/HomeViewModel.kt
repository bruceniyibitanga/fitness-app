package app.levitx.wear.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.session.SetLoad
import app.levitx.core.session.TrainingDay
import app.levitx.core.session.trainingDay
import app.levitx.data.DataUnavailableException
import app.levitx.data.equipment.Band
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.goal.GoalRepository
import app.levitx.data.progress.ProgressRepository
import app.levitx.data.session.SessionOutbox
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
 * Turns the goal, bests and the session in progress into [HomeUiState].
 *
 * The goal, bests and bands are copies of the phone's, kept in the watch's
 * own database by sync, so this screen works with the phone out of reach.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    goalRepository: GoalRepository,
    progressRepository: ProgressRepository,
    equipmentRepository: EquipmentRepository,
    outbox: SessionOutbox,
    private val controller: WorkoutController,
) : ViewModel() {
    // Typed on purpose: the branches below are different HomeUiState types,
    // and without the type the compiler can't tell what `catch` may emit.
    private val readyState: Flow<HomeUiState> = combine(
        goalRepository.observeActiveGoal(),
        progressRepository.observeBests(),
        controller.state,
        equipmentRepository.observeBands(),
        outbox.observeQueued(),
    ) { path, bests, workout, bands, queued ->
        when {
            workout is WorkoutState.Unavailable -> HomeUiState.Unavailable
            path == null -> HomeUiState.NoGoal
            else -> path.trainingDay(bests).toUiState(workout, bands, waitingToSync = queued.size)
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

    /** Starts today's session. The session screen shows it as soon as it is planned. */
    fun startSession() = controller.start()
}

private fun TrainingDay.toUiState(workout: WorkoutState, bands: List<Band>, waitingToSync: Int): HomeUiState.Ready {
    val idleLoad = (workout as? WorkoutState.Idle)?.load ?: SetLoad.None
    val tracksLoad = currentGate?.loadProfile != null
    return HomeUiState.Ready(
        skillName = path.name,
        gateTitle = currentGate?.title,
        gateNumber = gateNumber,
        gateCount = path.gates.size,
        today = plan?.let { plan ->
            val workSets = plan.sets.filterNot { it.isBestEffort }
            TodaySummary(
                workSets = workSets.size,
                workTarget = workSets.firstOrNull()?.target ?: 0,
                measure = plan.gate.measure,
            )
        },
        isSessionRunning = workout is WorkoutState.Running,
        // The load can be chosen before a session, for gates whose reps have a load to track.
        load = if (tracksLoad && workout is WorkoutState.Idle) {
            LoadSummary(
                addedKg = idleLoad.addedKg,
                bandName = bands.firstOrNull { idleLoad.assistanceKg > 0.0 && it.assistanceKg == idleLoad.assistanceKg }
                    ?.name,
            )
        } else {
            null
        },
        waitingToSync = waitingToSync,
    )
}
