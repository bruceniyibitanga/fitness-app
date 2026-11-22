package app.levitx.phone.pain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.coach.BodyArea
import app.levitx.core.pain.MAX_PAIN_RATING
import app.levitx.core.pain.MIN_PAIN_RATING
import app.levitx.core.pain.PainAdvice
import app.levitx.core.pain.PainCheckIn
import app.levitx.core.pain.RedFlag
import app.levitx.core.pain.assessPain
import app.levitx.data.coach.CoachRepository
import app.levitx.phone.handlingDataFailures
import app.levitx.phone.isExpectedDataFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Keep collecting through a rotation. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** The steps of a pain check, in order. */
enum class PainStep {
    /** Where it hurts. */
    AREA,

    /** Warning signs that mean a professional should look first. */
    SIGNS,

    /** How bad it is at its worst, 0 to 10. */
    RATING,
}

/** What the pain check shows. */
sealed interface PainCheckUiState {
    /** Still reading. */
    data object Loading : PainCheckUiState

    /** The encrypted data couldn't be opened or written. */
    data object Unavailable : PainCheckUiState

    /**
     * A check in progress.
     *
     * @property step The step on screen.
     * @property sore Areas the coach is already working around, with the advice for each.
     * @property area The area being checked, from the second step on.
     * @property signs Warning signs the person switched on.
     * @property rating Pain at its worst, 0 to 10, or null until a number is tapped.
     * @property saved True once the answer is stored, which closes the screen.
     */
    data class Ready(
        val step: PainStep,
        val sore: Map<BodyArea, PainAdvice>,
        val area: BodyArea? = null,
        val signs: Set<RedFlag> = emptySet(),
        val rating: Int? = null,
        val saved: Boolean = false,
    ) : PainCheckUiState {
        /** True when the step's main button can be pressed. */
        val canContinue: Boolean
            get() = when (step) {
                PainStep.AREA -> false
                PainStep.SIGNS -> area != null
                PainStep.RATING -> area != null && rating != null
            }
    }
}

/** The answers so far. */
private data class Draft(
    val step: PainStep = PainStep.AREA,
    val area: BodyArea? = null,
    val signs: Set<RedFlag> = emptySet(),
    val rating: Int? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val failed: Boolean = false,
)

/**
 * A pain check: where it hurts, any warning signs, then how bad it is. The
 * answers become advice through `assessPain`, and the coach plans around it
 * from the next session on.
 *
 * This is health data. Only the advice for the area is stored, in the
 * encrypted database: the rating and the warning signs are used once and
 * dropped. The answers in progress live in memory only, never in saved state,
 * because saved state is written to disk. If Android kills the app part way
 * through, the check starts again.
 */
@HiltViewModel
class PainCheckViewModel @Inject constructor(private val coachRepository: CoachRepository) : ViewModel() {
    private val draft = MutableStateFlow(Draft())

    /** The current screen state. */
    val uiState: StateFlow<PainCheckUiState> =
        combine(coachRepository.observeState(), draft) { state, draft ->
            when {
                draft.failed -> PainCheckUiState.Unavailable

                state == null -> PainCheckUiState.Loading

                else -> PainCheckUiState.Ready(
                    step = draft.step,
                    sore = state.model.pain,
                    area = draft.area,
                    signs = draft.signs,
                    rating = draft.rating,
                    saved = draft.saved,
                )
            }
        }
            .catch { error -> if (error.isExpectedDataFailure()) emit(PainCheckUiState.Unavailable) else throw error }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PainCheckUiState.Loading)

    /** Picks where it hurts and moves on to the warning signs. Starts the answers afresh. */
    fun chooseArea(area: BodyArea) {
        draft.update { if (it.saving || it.saved) it else Draft(step = PainStep.SIGNS, area = area) }
    }

    /** Switches a warning sign on or off. */
    fun toggleSign(sign: RedFlag) {
        draft.update { it.copy(signs = if (sign in it.signs) it.signs - sign else it.signs + sign) }
    }

    /** Sets pain at its worst. Ignores numbers off the scale; the screen doesn't offer them. */
    fun chooseRating(rating: Int) {
        if (rating !in MIN_PAIN_RATING..MAX_PAIN_RATING) return
        draft.update { it.copy(rating = rating) }
    }

    /**
     * The step's main button. With a warning sign switched on, the area is
     * paused straight away and no rating is asked: a warning sign decides it.
     * Without one, the rating comes next, and then the answer is stored.
     */
    fun next() {
        val now = draft.value
        val area = now.area ?: return
        when (now.step) {
            PainStep.AREA -> Unit

            PainStep.SIGNS -> if (now.signs.isEmpty()) {
                draft.update { it.copy(step = PainStep.RATING) }
            } else {
                save(area, PainCheckIn(duringExercise = MIN_PAIN_RATING, redFlags = now.signs))
            }

            PainStep.RATING -> now.rating?.let { save(area, PainCheckIn(duringExercise = it, redFlags = now.signs)) }
        }
    }

    /** Goes back a step. False on the first step, where back leaves the screen. */
    fun back(): Boolean {
        val now = draft.value
        if (now.saving || now.step == PainStep.AREA) return false
        draft.update {
            when (it.step) {
                PainStep.RATING -> it.copy(step = PainStep.SIGNS)
                else -> Draft()
            }
        }
        return true
    }

    private fun save(area: BodyArea, checkIn: PainCheckIn) {
        // Marked before the write starts, so a second tap can't store the answer twice.
        if (draft.value.saving || draft.value.saved) return
        draft.update { it.copy(saving = true) }
        viewModelScope.launch {
            handlingDataFailures(onFailure = { draft.update { it.copy(saving = false, failed = true) } }) {
                coachRepository.recordPain(area, assessPain(checkIn))
                draft.update { it.copy(saving = false, saved = true) }
            }
        }
    }
}
