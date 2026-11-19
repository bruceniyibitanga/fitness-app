package app.levitx.phone.live

import android.content.Context
import app.levitx.core.intervals.IntervalPhase
import app.levitx.phone.R

/**
 * The line of text the notification shows for a cardio or mobility workout:
 * the step it is on. Like the strength workout's, it names moves and rounds
 * only, so it can show on a locked phone.
 */
internal fun LiveIntervals.noticeText(context: Context): String = when {
    paused -> context.getString(R.string.live_interval_paused)

    else -> when (phase) {
        IntervalPhase.PREPARE -> context.getString(R.string.live_interval_ready, moveName)
        IntervalPhase.WORK -> context.getString(R.string.live_interval_work, moveName, round, rounds)
        IntervalPhase.SWITCH -> context.getString(R.string.live_interval_switch)
        IntervalPhase.REST -> context.getString(R.string.live_title_rest)
    }
}
