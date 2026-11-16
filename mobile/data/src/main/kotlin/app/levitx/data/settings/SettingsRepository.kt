package app.levitx.data.settings

import kotlinx.coroutines.flow.Flow

/** Count-in lengths the person can choose, in seconds. Three is the default. */
val LEAD_IN_CHOICES: List<Int> = listOf(3, 5, 10)

/**
 * How the person likes the app to behave. Nothing here is personal data,
 * which is why it lives outside the encrypted database.
 *
 * @property leadInSeconds The count-in before a hold or negatives, one of [LEAD_IN_CHOICES].
 * @property sounds True when timers and rest beep.
 * @property buzzes True when timers and rest buzz.
 * @property notificationsAsked True once the app has asked to show the workout
 *   notification. It asks once; after that the choice is changed in Android's settings.
 */
data class AppSettings(
    val leadInSeconds: Int = LEAD_IN_CHOICES.first(),
    val sounds: Boolean = true,
    val buzzes: Boolean = true,
    val notificationsAsked: Boolean = false,
)

/** Reads and changes [AppSettings]. */
interface SettingsRepository {
    /** The settings, emitted again when they change. Defaults until the person changes one. */
    fun observe(): Flow<AppSettings>

    /** Sets the count-in before holds and negatives. Values outside [LEAD_IN_CHOICES] are ignored. */
    suspend fun setLeadInSeconds(seconds: Int)

    /** Turns timer and rest beeps on or off. */
    suspend fun setSounds(on: Boolean)

    /** Turns timer and rest buzzes on or off. */
    suspend fun setBuzzes(on: Boolean)

    /** Records that the app has asked to show the workout notification, so it doesn't ask again. */
    suspend fun markNotificationsAsked()

    /** Puts every setting back to its default. */
    suspend fun clear()
}
