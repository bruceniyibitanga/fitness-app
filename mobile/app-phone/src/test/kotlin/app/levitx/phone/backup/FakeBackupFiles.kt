package app.levitx.phone.backup

import app.levitx.data.backup.RestoreResult
import java.io.IOException

/** [BackupFiles] that records what it was asked and answers as set. */
class FakeBackupFiles : BackupFiles {
    /** The words each write was asked to lock with. */
    val written = mutableListOf<Pair<String, List<String>>>()

    /** The words each restore was tried with. */
    val restored = mutableListOf<Pair<String, List<String>>>()

    /** When true, writes fail as a full disk would. */
    var writeFails = false

    /** What restores answer, or null to fail as an unreadable file would. */
    var restoreResult: RestoreResult? = RestoreResult.Restored

    override suspend fun write(uri: String, words: List<String>) {
        if (writeFails) throw IOException("full")
        written += uri to words
    }

    override suspend fun restore(uri: String, words: List<String>): RestoreResult {
        restored += uri to words
        return restoreResult ?: throw IOException("unreadable")
    }
}
