package app.levitx.wear.tile

import android.content.Context
import androidx.wear.tiles.TileService
import app.levitx.core.session.trainingDay
import app.levitx.data.ApplicationScope
import app.levitx.data.DataUnavailableException
import app.levitx.data.goal.GoalRepository
import app.levitx.data.progress.ProgressRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Asks the system to redraw the Tile whenever what it shows has changed: a
 * new goal from the phone, or a best raised by a session.
 *
 * Without this the Tile would only catch up when its freshness interval runs
 * out. The system decides when to actually redraw, and skips it if the Tile
 * isn't in use, so asking is cheap.
 */
@Singleton
class TileRefresher @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val goalRepository: GoalRepository,
    private val progressRepository: ProgressRepository,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    /** Starts watching for changes. Call once, when the app process starts. */
    fun start() {
        scope.launch {
            combine(goalRepository.observeActiveGoal(), progressRepository.observeBests()) { path, bests ->
                path?.trainingDay(bests)
            }
                .map { tileContentFor(it) }
                .distinctUntilChanged()
                // Locked data is expected and shown in the app. Anything else is a bug.
                .catch { error -> if (error !is DataUnavailableException) throw error }
                .collect { TileService.getUpdater(context).requestUpdate(NextSessionTileService::class.java) }
        }
    }
}
