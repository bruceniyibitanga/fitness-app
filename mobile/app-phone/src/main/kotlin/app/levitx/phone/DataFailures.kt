package app.levitx.phone

import app.levitx.data.DataUnavailableException
import app.levitx.data.coach.UnreadableCoachStateException

/**
 * The failures a screen explains instead of crashing: data locked on this
 * device, or saved by a newer version of the app. Anything else is a bug and
 * should crash loudly rather than hide behind a message.
 */
internal fun Throwable.isExpectedDataFailure(): Boolean =
    this is DataUnavailableException || this is UnreadableCoachStateException

/**
 * Runs [block], and runs [onFailure] instead if the data turns out to be
 * locked or unreadable. Other errors aren't caught.
 */
internal inline fun <T> handlingDataFailures(onFailure: () -> T, block: () -> T): T = try {
    block()
} catch (_: DataUnavailableException) {
    onFailure()
} catch (_: UnreadableCoachStateException) {
    onFailure()
}
