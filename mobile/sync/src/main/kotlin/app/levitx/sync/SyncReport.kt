package app.levitx.sync

import android.util.Log

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
