package app.levitx.testing

import app.levitx.core.load.WeightUnit
import app.levitx.data.settings.AppSettings
import app.levitx.data.settings.LEAD_IN_CHOICES
import app.levitx.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [SettingsRepository]. [settings] is the current value, for assertions. */
class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    /** The settings now. */
    val settings = MutableStateFlow(initial)

    override fun observe(): Flow<AppSettings> = settings.asStateFlow()

    override suspend fun setLeadInSeconds(seconds: Int) {
        if (seconds in LEAD_IN_CHOICES) settings.update { it.copy(leadInSeconds = seconds) }
    }

    override suspend fun setSounds(on: Boolean) {
        settings.update { it.copy(sounds = on) }
    }

    override suspend fun setBuzzes(on: Boolean) {
        settings.update { it.copy(buzzes = on) }
    }

    override suspend fun setUnits(unit: WeightUnit) {
        settings.update { it.copy(units = unit) }
    }

    override suspend fun markNotificationsAsked() {
        settings.update { it.copy(notificationsAsked = true) }
    }

    override suspend fun clear() {
        settings.value = AppSettings()
    }
}
