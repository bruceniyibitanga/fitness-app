package app.levitx.phone.equipment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.load.LoadLimits
import app.levitx.data.DataUnavailableException
import app.levitx.data.equipment.Band
import app.levitx.data.equipment.EquipmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the equipment screen shows. */
sealed interface EquipmentUiState {
    /** Loading. */
    data object Loading : EquipmentUiState

    /** Data couldn't be unlocked. */
    data object Unavailable : EquipmentUiState

    /**
     * The person's bodyweight and bands.
     *
     * @property bodyweightKg Latest bodyweight, or null before one is entered.
     * @property bands Bands, oldest first.
     */
    data class Ready(val bodyweightKg: Double?, val bands: List<Band>) : EquipmentUiState
}

/** Bodyweight and bands: what the app needs to work out the load of each rep. */
@HiltViewModel
class EquipmentViewModel @Inject constructor(private val equipment: EquipmentRepository) : ViewModel() {
    /** The current screen state. */
    val uiState: StateFlow<EquipmentUiState> =
        combine(equipment.observeBodyweightKg(), equipment.observeBands()) { bodyweight, bands ->
            EquipmentUiState.Ready(bodyweight, bands)
        }
            .catch { error ->
                if (error is DataUnavailableException) emit(EquipmentUiState.Unavailable) else throw error
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), EquipmentUiState.Loading)

    /** Saves a bodyweight reading. Ignores values outside 20 to 350 kg; the screen won't offer them. */
    fun saveBodyweight(kg: Double) {
        if (!isValidBodyweight(kg)) return
        viewModelScope.launch { equipment.recordBodyweight(kg) }
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

    private fun isValidBodyweight(kg: Double): Boolean =
        kg in LoadLimits.MIN_BODYWEIGHT_KG..LoadLimits.MAX_BODYWEIGHT_KG
}

private const val STOP_TIMEOUT_MILLIS = 5_000L
