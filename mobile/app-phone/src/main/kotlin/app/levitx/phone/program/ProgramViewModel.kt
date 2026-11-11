package app.levitx.phone.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.skills.SkillCatalogue
import app.levitx.core.skills.SkillPath
import app.levitx.data.coach.CoachRepository
import app.levitx.phone.handlingDataFailures
import app.levitx.phone.isExpectedDataFailure
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Keep collecting for this long after the screen stops listening, so a rotation doesn't restart the flow. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** What a skill program's screen shows. */
sealed interface ProgramUiState {
    /** Loading. */
    data object Loading : ProgramUiState

    /** The data couldn't be read on this device. */
    data object Unavailable : ProgramUiState

    /** No program has this id (a back stack saved by an older version). */
    data object NotFound : ProgramUiState

    /**
     * The program and the person's progress through it.
     *
     * @property path The program.
     * @property bestByGateId The person's best per gate.
     * @property currentIndex The gate being worked on, or null when every gate is passed.
     * @property isPlan True when it is already the person's goal.
     */
    data class Ready(
        val path: SkillPath,
        val bestByGateId: Map<String, Int>,
        val currentIndex: Int?,
        val isPlan: Boolean,
    ) : ProgramUiState
}

/**
 * A skill program from the Train tab, and the choice to make it the plan.
 *
 * Making it the plan changes only the goal. Everything the coach has learned,
 * every best and every weigh-in stays.
 */
@HiltViewModel(assistedFactory = ProgramViewModel.Factory::class)
class ProgramViewModel @AssistedInject constructor(
    @Assisted private val pathId: String,
    private val coachRepository: CoachRepository,
) : ViewModel() {
    /** The current screen state. */
    val uiState: StateFlow<ProgramUiState> = coachRepository.observeState()
        .map { state ->
            val path = SkillCatalogue.byId(pathId)
            when {
                path == null -> ProgramUiState.NotFound

                state == null -> ProgramUiState.Loading

                else -> ProgramUiState.Ready(
                    path = path,
                    bestByGateId = state.model.gateBests,
                    currentIndex = path.progressFor(state.model.gateBests).currentIndex,
                    isPlan = state.model.profile.goalPathId == path.id,
                )
            }
        }
        .catch { error -> if (error.isExpectedDataFailure()) emit(ProgramUiState.Unavailable) else throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProgramUiState.Loading)

    /**
     * Makes this program the person's goal, keeping their plan style, week and kit.
     *
     * @return True when it was saved.
     */
    suspend fun makePlan(): Boolean = handlingDataFailures(onFailure = { false }) {
        val state = coachRepository.observeState().first()
        if (state == null || SkillCatalogue.byId(pathId) == null) {
            false
        } else {
            coachRepository.updateProfile(state.model.profile.copy(goalPathId = pathId), state.trainingDays)
            true
        }
    }

    /** Makes a [ProgramViewModel] for one program. */
    @AssistedFactory
    interface Factory {
        /** The ViewModel for the program with [pathId]. */
        fun create(pathId: String): ProgramViewModel
    }
}
