package app.levitx.sync

import android.util.Log
import app.levitx.data.DataUnavailableException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "LevitxSync"

/** Longest a sync pass may run inside a listener service callback. A pass normally takes well under a second. */
internal const val LISTENER_TIMEOUT_MILLIS = 15_000L

/** How a sync pass ended. */
internal enum class SyncOutcome {
    /** The pass ran. */
    DONE,

    /** The Data Layer isn't available on this device right now. */
    NO_LINK,

    /** The encrypted database couldn't be unlocked. */
    DATA_LOCKED,
}

/**
 * What one sync pass did. It holds counts only, never the data itself, so it
 * is safe to log.
 *
 * @property outcome How the pass ended.
 * @property delivered Sessions stored (on the phone) or confirmed delivered (on the watch) in this pass.
 * @property skipped Items left alone because they were unreadable or from a newer app version.
 * @property stateChanged True when the phone published new state, or the watch applied it.
 */
internal data class SyncReport(
    val outcome: SyncOutcome,
    val delivered: Int = 0,
    val skipped: Int = 0,
    val stateChanged: Boolean = false,
)

/** Writes the report to the log. Counts and codes only. */
internal fun SyncReport.log(pass: String) {
    Log.i(TAG, "$pass: outcome=$outcome delivered=$delivered skipped=$skipped stateChanged=$stateChanged")
}

/**
 * Runs one sync [pass] on its own, after any pass already holding this lock,
 * and reports the two expected ways it can fail instead of throwing them: no
 * Data Layer on this device right now, and data that can't be read (locked,
 * or saved by a newer version of the app). Anything else is a bug and passes
 * through.
 */
internal suspend fun Mutex.syncPass(pass: suspend () -> SyncReport): SyncReport = withLock {
    try {
        pass()
    } catch (error: DataLayerUnavailableException) {
        SyncReport(SyncOutcome.NO_LINK)
    } catch (error: DataUnavailableException) {
        SyncReport(SyncOutcome.DATA_LOCKED)
    }
}
