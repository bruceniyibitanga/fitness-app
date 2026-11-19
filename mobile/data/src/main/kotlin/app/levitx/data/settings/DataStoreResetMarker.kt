package app.levitx.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.levitx.data.privacy.ResetMarker
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Where the reset id is kept. The settings repository leaves this one key alone when it clears. */
internal val RESET_ID = stringPreferencesKey("last_reset_id")

/**
 * [ResetMarker] in the settings file. It holds one random id and nothing
 * about the person, so it needs no encryption. Clearing the settings leaves
 * it alone: it has to outlive "delete everything" to do its job.
 */
@Singleton
internal class DataStoreResetMarker @Inject constructor(private val store: DataStore<Preferences>) : ResetMarker {
    override suspend fun current(): String? = store.data.first()[RESET_ID]

    override suspend fun set(resetId: String) {
        store.edit { it[RESET_ID] = resetId }
    }
}
