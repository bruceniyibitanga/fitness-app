package app.levitx.wear.load

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.load.LoadLimits
import app.levitx.core.session.SetLoad
import app.levitx.data.DataUnavailableException
import app.levitx.data.equipment.Band
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.wear.session.WorkoutController
import app.levitx.wear.session.WorkoutState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * One tap of plus or minus, in kilograms. Whole kilograms keep a 10 kg vest
 * to ten taps on a small screen; half kilograms are entered on the phone.
 */
internal const val ADDED_KG_STEP = 1.0

/**
 * What the load screen shows.
 *
 * @property addedKg Vest or belt weight for the next session.
 * @property assistanceKg Band help for the next session. 0 for no band.
 * @property bands The person's bands, copied from the phone.
 */
data class LoadUiState(
    val addedKg: Double = 0.0,
    val assistanceKg: Double = 0.0,
    val bands: List<Band> = emptyList(),
) {
    /** False at the lowest added weight. */
    val canDecrease: Boolean
        get() = addedKg > 0.0

    /** False at the highest added weight. */
    val canIncrease: Boolean
        get() = addedKg < LoadLimits.MAX_MODIFIER_KG
}

/**
 * Chooses the vest, belt or band for the next session's skill work.
 *
 * The choice is kept by the [WorkoutController], which stamps it on every set
 * of the skill work. Bands are set up and calibrated on the phone and arrive
 * here through sync.
 */
@HiltViewModel
class LoadViewModel @Inject constructor(
    private val controller: WorkoutController,
    equipmentRepository: EquipmentRepository,
) : ViewModel() {
    /** The current screen state. */
    val uiState: StateFlow<LoadUiState> =
        combine(controller.state, equipmentRepository.observeBands()) { workout, bands ->
            val load = (workout as? WorkoutState.Idle)?.load ?: SetLoad.None
            LoadUiState(addedKg = load.addedKg, assistanceKg = load.assistanceKg, bands = bands)
        }
            // Without the database there are no bands to offer; added weight still works.
            .catch { error -> if (error is DataUnavailableException) emit(LoadUiState()) else throw error }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LoadUiState())

    /** One step more added weight. */
    fun increaseAdded() = changeAddedBy(ADDED_KG_STEP)

    /** One step less added weight. */
    fun decreaseAdded() = changeAddedBy(-ADDED_KG_STEP)

    /** Chooses [band] for the next session's skill work, or no band when null. */
    fun chooseBand(band: Band?) {
        val current = uiState.value
        controller.setLoad(SetLoad(addedKg = current.addedKg, assistanceKg = band?.assistanceKg ?: 0.0))
    }

    private fun changeAddedBy(stepKg: Double) {
        val current = uiState.value
        val addedKg = (current.addedKg + stepKg).coerceIn(0.0, LoadLimits.MAX_MODIFIER_KG)
        controller.setLoad(SetLoad(addedKg = addedKg, assistanceKg = current.assistanceKg))
    }
}
