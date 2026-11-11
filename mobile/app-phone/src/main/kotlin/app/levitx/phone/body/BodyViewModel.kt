package app.levitx.phone.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.coach.Equipment
import app.levitx.core.load.LoadLimits
import app.levitx.data.coach.CoachRepository
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.time.TrainingClock
import app.levitx.phone.isExpectedDataFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Keep collecting through a rotation. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * The Body tab: bodyweight and its trend, load per rep, the kit the plan can
 * use, and calibrated bands.
 *
 * Bodyweight is health data. It is read from and written to the encrypted
 * database on this phone only, and never leaves it.
 */
@HiltViewModel
class BodyViewModel @Inject constructor(
    private val equipment: EquipmentRepository,
    private val coachRepository: CoachRepository,
    private val clock: TrainingClock,
) : ViewModel() {
    // Two quick taps on the kit would otherwise both read the old kit and the second would undo the first.
    private val kitLock = Mutex()

    /** The current screen state. */
    val uiState: StateFlow<BodyUiState> =
        combine(coachRepository.observeState(), equipment.observeBands()) { state, bands ->
            if (state == null) BodyUiState.Loading else bodyState(state.model, bands, clock.currentTimeMillis())
        }
            .catch { error -> if (error.isExpectedDataFailure()) emit(BodyUiState.Unavailable) else throw error }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), BodyUiState.Loading)

    /** Saves a weigh-in. Ignores values outside 20 to 350 kg; the screen won't offer them. */
    fun saveBodyweight(kg: Double) {
        if (!kg.isFinite() || kg !in LoadLimits.MIN_BODYWEIGHT_KG..LoadLimits.MAX_BODYWEIGHT_KG) return
        viewModelScope.launch { equipment.recordBodyweight(kg) }
    }

    /**
     * Changes the kit the plan can use: null means none at all, anything else
     * is added or taken away. The coach plans from the new kit straight away.
     */
    fun chooseEquipment(item: Equipment?) {
        viewModelScope.launch {
            kitLock.withLock {
                val state = coachRepository.observeState().first() ?: return@withLock
                val profile = state.model.profile
                val kit = when (item) {
                    null -> emptySet()
                    in profile.equipment -> profile.equipment - item
                    else -> profile.equipment + item
                }
                coachRepository.updateProfile(profile.copy(equipment = kit), state.trainingDays)
            }
        }
    }

    /** Adds a calibrated band. Ignores a blank name or impossible help; the screen won't offer them. */
    fun addBand(name: String, assistanceKg: Double) {
        if (name.isBlank() || assistanceKg <= 0.0 || assistanceKg > LoadLimits.MAX_MODIFIER_KG) return
        viewModelScope.launch { equipment.addBand(name, assistanceKg) }
    }

    /** Removes a band. */
    fun deleteBand(id: String) {
        viewModelScope.launch { equipment.deleteBand(id) }
    }
}
