package app.levitx.phone.backup

import android.content.Context
import androidx.core.net.toUri
import app.levitx.data.IoDispatcher
import app.levitx.data.backup.BackupRepository
import app.levitx.data.backup.RestoreResult
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.FileNotFoundException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Backup files the person picks in the system's save and open dialogs. */
interface BackupFiles {
    /**
     * Writes a backup locked with [words] to the document at [uri].
     *
     * @throws java.io.IOException if the file can't be written.
     */
    suspend fun write(uri: String, words: List<String>)

    /**
     * Restores the backup at [uri] with [words].
     *
     * @throws java.io.IOException if the file can't be read.
     */
    suspend fun restore(uri: String, words: List<String>): RestoreResult
}

/**
 * [BackupFiles] through the system's document provider: the person chose the
 * file, so the app needs no storage permission and touches only that file.
 */
internal class DocumentBackupFiles @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val backups: BackupRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BackupFiles {
    override suspend fun write(uri: String, words: List<String>) {
        val out = withContext(ioDispatcher) {
            // "wt": write, and cut off anything a file the person chose to replace held before.
            context.contentResolver.openOutputStream(uri.toUri(), "wt")
        } ?: throw FileNotFoundException("The chosen file couldn't be opened")
        out.use { backups.write(it, words) }
    }

    override suspend fun restore(uri: String, words: List<String>): RestoreResult {
        val input = withContext(ioDispatcher) { context.contentResolver.openInputStream(uri.toUri()) }
            ?: throw FileNotFoundException("The chosen file couldn't be opened")
        return input.use { backups.restore(it, words) }
    }
}

/** Binds [BackupFiles] for the backup and restore screens. */
@Module
@InstallIn(SingletonComponent::class)
internal interface BackupFilesModule {
    @Binds
    fun bindBackupFiles(files: DocumentBackupFiles): BackupFiles
}
