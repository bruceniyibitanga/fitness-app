package app.levitx.wear.tile

import app.levitx.core.coach.PlannedSession
import app.levitx.wear.home.NextSummary
import app.levitx.wear.home.summary

/**
 * What the Tile shows, worked out with the same [summary] the home screen
 * uses, so the two always agree.
 */
sealed interface TileContent {
    /** The database couldn't be read. The Tile points to the app, which explains why. */
    data object Unavailable : TileContent

    /** No session on the watch yet. It arrives from the phone. */
    data object NoSession : TileContent

    /**
     * The next session.
     *
     * @property summary The session in a glance.
     */
    data class NextSession(val summary: NextSummary) : TileContent
}

/** The Tile's content for [next], the session the phone's coach planned, or null for none. */
fun tileContentFor(next: PlannedSession?): TileContent =
    next?.let { TileContent.NextSession(it.summary()) } ?: TileContent.NoSession
