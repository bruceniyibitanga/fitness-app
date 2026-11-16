package app.levitx.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import app.levitx.data.ApplicationScope
import app.levitx.data.IoDispatcher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/** File name of the settings store in the app's private files. */
private const val SETTINGS_FILE = "settings"

/** Provides the settings store and binds [SettingsRepository]. */
@Module
@InstallIn(SingletonComponent::class)
internal interface SettingsModule {
    @Binds
    fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository

    companion object {
        /**
         * One DataStore for the settings file, for the life of the app: two
         * instances on the same file would corrupt it. Settings are only
         * conveniences, so a file that can't be read starts again from the
         * defaults.
         */
        @Provides
        @Singleton
        fun provideSettingsStore(
            @ApplicationContext context: Context,
            @ApplicationScope scope: CoroutineScope,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = scope + ioDispatcher,
            produceFile = { context.preferencesDataStoreFile(SETTINGS_FILE) },
        )
    }
}
