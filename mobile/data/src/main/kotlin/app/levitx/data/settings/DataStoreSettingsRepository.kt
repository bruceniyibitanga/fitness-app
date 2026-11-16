package app.levitx.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.levitx.core.load.WeightUnit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val LEAD_IN = intPreferencesKey("lead_in_seconds")
private val SOUNDS = booleanPreferencesKey("sounds")
private val BUZZES = booleanPreferencesKey("buzzes")
private val NOTIFICATIONS_ASKED = booleanPreferencesKey("notifications_asked")
private val UNITS = stringPreferencesKey("units")

/** Countries where people weigh themselves and their plates in pounds. */
private val POUND_COUNTRIES = setOf("US", "LR", "MM")

/** Pounds where the phone is set to a pound country, kilograms everywhere else, until the person picks. */
private fun defaultUnits(): WeightUnit =
    if (Locale.getDefault().country in POUND_COUNTRIES) WeightUnit.LB else WeightUnit.KG

/**
 * [SettingsRepository] on Jetpack DataStore. DataStore reads and writes off
 * the main thread by itself, and a file it can't read comes back as the
 * defaults rather than crashing (see the module's corruption handler).
 */
@Singleton
internal class DataStoreSettingsRepository @Inject constructor(private val store: DataStore<Preferences>) :
    SettingsRepository {
    override fun observe(): Flow<AppSettings> = store.data
        .map { prefs ->
            AppSettings(
                leadInSeconds = prefs[LEAD_IN]?.takeIf { it in LEAD_IN_CHOICES } ?: LEAD_IN_CHOICES.first(),
                sounds = prefs[SOUNDS] ?: true,
                buzzes = prefs[BUZZES] ?: true,
                notificationsAsked = prefs[NOTIFICATIONS_ASKED] ?: false,
                units = prefs[UNITS]?.let { stored -> WeightUnit.entries.firstOrNull { it.name == stored } }
                    ?: defaultUnits(),
            )
        }
        .distinctUntilChanged()

    override suspend fun setLeadInSeconds(seconds: Int) {
        if (seconds in LEAD_IN_CHOICES) store.edit { it[LEAD_IN] = seconds }
    }

    override suspend fun setSounds(on: Boolean) {
        store.edit { it[SOUNDS] = on }
    }

    override suspend fun setBuzzes(on: Boolean) {
        store.edit { it[BUZZES] = on }
    }

    override suspend fun setUnits(unit: WeightUnit) {
        store.edit { it[UNITS] = unit.name }
    }

    override suspend fun markNotificationsAsked() {
        store.edit { it[NOTIFICATIONS_ASKED] = true }
    }

    override suspend fun clear() {
        store.edit { it.clear() }
    }
}
