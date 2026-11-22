package app.levitx.data.backup

import java.io.InputStream
import java.io.OutputStream

/** What a restore did. */
public sealed interface RestoreResult {
    /** Everything on the phone was replaced with the backup. */
    public data object Restored : RestoreResult

    /** A workout is in progress, so nothing was touched. */
    public data object WorkoutInProgress : RestoreResult

    /**
     * The backup couldn't be used, and nothing was touched.
     *
     * @property problem Why.
     */
    public data class Failed(val problem: BackupProblem) : RestoreResult
}

/** The person's own encrypted backup file (ADR 0009). */
public interface BackupRepository {
    /**
     * Writes everything worth keeping to [out] as a backup locked with
     * [words]. Doesn't close [out].
     *
     * @throws app.levitx.data.DataUnavailableException if the data can't be read.
     * @throws java.io.IOException if [out] can't be written.
     */
    public suspend fun write(out: OutputStream, words: List<String>)

    /**
     * Reads a backup from [input] and, if [words] open it and everything in
     * it checks out, replaces the person's data with it, all at once. When
     * anything is wrong nothing on the phone changes. Doesn't close [input].
     *
     * @throws app.levitx.data.DataUnavailableException if the data can't be written.
     * @throws java.io.IOException if [input] can't be read.
     */
    public suspend fun restore(input: InputStream, words: List<String>): RestoreResult
}
