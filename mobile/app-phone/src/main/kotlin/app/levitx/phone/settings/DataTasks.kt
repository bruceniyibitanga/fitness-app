package app.levitx.phone.settings

import android.content.Context
import androidx.core.net.toUri
import app.levitx.data.IoDispatcher
import app.levitx.data.privacy.PrivacyRepository
import app.levitx.data.settings.SettingsRepository
import app.levitx.sync.PhoneSync
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.FileNotFoundException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Writes the person's export to a file they chose. */
interface DataExport {
    /**
     * Writes the export to the document at [uri], which the person picked in
     * the system's save dialog.
     *
     * @throws java.io.IOException if the file can't be written.
     */
    suspend fun exportTo(uri: String)
}

/** Deletes everything Levitx keeps about the person on this phone. */
interface DataEraser {
    /** Deletes what the phone offers the watch, the database's rows and the settings. Runs to the end once begun. */
    suspend fun eraseEverything()
}

/**
 * [DataExport] through the system's document provider. The person chose the
 * file in the save dialog, so the app needs no storage permission and can
 * only write that one file.
 */
internal class DocumentDataExport @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val privacy: PrivacyRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DataExport {
    override suspend fun exportTo(uri: String) {
        withContext(ioDispatcher) {
            // "wt": write, and cut off anything a file the person chose to replace held before.
            val out = context.contentResolver.openOutputStream(uri.toUri(), "wt")
                ?: throw FileNotFoundException("The chosen file couldn't be opened")
            out.use { privacy.export(it) }
        }
    }
}

/**
 * [DataEraser] for the phone: the watch link, the database, then the settings.
 *
 * Once begun it runs to the end, even if the person leaves the screen or the
 * screen is torn down: half a delete helps nobody. The watch is told first,
 * because that step saves the delete's id before anything is gone. If the
 * app dies partway, the id is still there at the next start and the watch is
 * told then (see [PhoneSync.forget]).
 */
internal class AppDataEraser @Inject constructor(
    private val privacy: PrivacyRepository,
    private val settings: SettingsRepository,
    private val phoneSync: PhoneSync,
) : DataEraser {
    override suspend fun eraseEverything() {
        withContext(NonCancellable) {
            phoneSync.forget()
            privacy.deleteEverything()
            settings.clear()
        }
    }
}

/** Binds the export and the eraser for the settings screen. */
@Module
@InstallIn(SingletonComponent::class)
internal interface DataTasksModule {
    @Binds
    fun bindDataExport(export: DocumentDataExport): DataExport

    @Binds
    fun bindDataEraser(eraser: AppDataEraser): DataEraser
}
