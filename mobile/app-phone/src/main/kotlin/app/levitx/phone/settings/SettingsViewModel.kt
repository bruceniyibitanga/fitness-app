package app.levitx.phone.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.load.WeightUnit
import app.levitx.data.settings.AppSettings
import app.levitx.data.settings.SettingsRepository
import app.levitx.phone.handlingDataFailures
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Keep collecting through a rotation. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** Where an export or a delete has got to. */
enum class DataTask {
    /** Nothing running. */
    IDLE,

    /** Writing the export. */
    EXPORTING,

    /** The export was written. */
    EXPORTED,

    /** The export couldn't be written. Nothing was changed. */
    EXPORT_FAILED,

    /** Deleting everything. */
    DELETING,

    /** Everything is deleted. Time to start again from setup. */
    DELETED,

    /** Deleting failed partway, or couldn't start. */
    DELETE_FAILED,
}

/**
 * The settings screen.
 *
 * @property settings The settings.
 * @property task Where an export or a delete has got to.
 */
data class SettingsUiState(val settings: AppSettings, val task: DataTask)

/** Settings, the person's data export and deleting everything. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val export: DataExport,
    private val eraser: DataEraser,
) : ViewModel() {
    private val task = MutableStateFlow(DataTask.IDLE)

    /** The current screen state. */
    val uiState: StateFlow<SettingsUiState> = combine(settings.observe(), task, ::SettingsUiState)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            SettingsUiState(AppSettings(), DataTask.IDLE),
        )

    /** Sets the count-in before holds and negatives. */
    fun setLeadInSeconds(seconds: Int) {
        viewModelScope.launch { settings.setLeadInSeconds(seconds) }
    }

    /** Shows and takes weights in [unit] from now on. */
    fun setUnits(unit: WeightUnit) {
        viewModelScope.launch { settings.setUnits(unit) }
    }

    /** Turns beeps on or off. */
    fun setSounds(on: Boolean) {
        viewModelScope.launch { settings.setSounds(on) }
    }

    /** Turns buzzes on or off. */
    fun setBuzzes(on: Boolean) {
        viewModelScope.launch { settings.setBuzzes(on) }
    }

    /** Writes the export to [uri], the file the person chose. One at a time. */
    fun export(uri: String) {
        if (task.value == DataTask.EXPORTING || task.value == DataTask.DELETING) return
        task.value = DataTask.EXPORTING
        viewModelScope.launch {
            task.value = handlingDataFailures(onFailure = { DataTask.EXPORT_FAILED }) {
                try {
                    export.exportTo(uri)
                    DataTask.EXPORTED
                } catch (_: IOException) {
                    // The file couldn't be written (full storage, the provider went away).
                    // The screen says so; nothing in the app changed.
                    DataTask.EXPORT_FAILED
                }
            }
        }
    }

    /** Deletes everything, after the person confirmed it. */
    fun deleteEverything() {
        if (task.value == DataTask.DELETING || task.value == DataTask.DELETED) return
        task.value = DataTask.DELETING
        viewModelScope.launch {
            task.value = handlingDataFailures(onFailure = { DataTask.DELETE_FAILED }) {
                try {
                    eraser.eraseEverything()
                    DataTask.DELETED
                } catch (_: IOException) {
                    // The settings file couldn't be written. The screen says so and the person can try again.
                    DataTask.DELETE_FAILED
                }
            }
        }
    }
}
