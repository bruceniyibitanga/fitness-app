package app.levitx.phone.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How long a screen can be gone, as on a rotation, before the settings stop being read. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Whether to ask, once, to show the workout notification.
 *
 * Asking every workout until the person gives in would be nagging. After the
 * first answer, it is changed in Android's settings, which Settings links to.
 */
@HiltViewModel
class NotificationPromptViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    /** True until the app has asked. False while the settings are read, so it never asks twice by accident. */
    val shouldAsk: StateFlow<Boolean> = settings.observe()
        .map { !it.notificationsAsked }
        // Settings that can't be read mean not asking: the workout runs the same either way.
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    /** Records that the app asked. */
    fun asked() {
        viewModelScope.launch { settings.markNotificationsAsked() }
    }
}
