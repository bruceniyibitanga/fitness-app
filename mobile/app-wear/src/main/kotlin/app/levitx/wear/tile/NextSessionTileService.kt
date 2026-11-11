package app.levitx.wear.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import app.levitx.data.DataUnavailableException
import app.levitx.data.coach.NextSessionRepository
import app.levitx.wear.MainActivity
import app.levitx.wear.R
import app.levitx.wear.home.NextSummary
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future

/** The Tile uses no images, so its resources never change. */
private const val RESOURCES_VERSION = "1"

/**
 * How long the system may show a Tile before asking for a new one. The plan
 * changes after a workout, so a quarter of an hour is fresh enough and costs
 * almost nothing. A finished workout also asks for a refresh (TileRefresher).
 */
private const val FRESHNESS_MILLIS = 15 * 60 * 1_000L

private const val OPEN_APP_CLICK_ID = "open_app"

/** Exercise names and messages may wrap, but not push the rest of the Tile off the screen. */
private const val MAIN_TEXT_MAX_LINES = 3

/**
 * The "Next session" Tile: the session the phone's coach planned, one swipe
 * from the watch face. Tapping it opens the app.
 *
 * It reads the same plan as the home screen. It shows what the session holds
 * only, never results or anything about the person's health, because a Tile
 * is visible to anyone who can see the watch.
 */
@AndroidEntryPoint
class NextSessionTileService : TileService() {
    /** The session the phone's coach planned. Set by Hilt. */
    @Inject
    lateinit var nextSessionRepository: NextSessionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        // The Tiles API wants a future; the database read is a suspend call. `future` joins the two.
        scope.future {
            val layout = layout(requestParams, loadContent())
            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setFreshnessIntervalMillis(FRESHNESS_MILLIS)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
                .build()
        }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> =
        scope.future { ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build() }

    private suspend fun loadContent(): TileContent = try {
        tileContentFor(nextSessionRepository.observeNext().first())
    } catch (error: DataUnavailableException) {
        TileContent.Unavailable
    }

    private fun layout(requestParams: RequestBuilders.TileRequest, content: TileContent): LayoutElement =
        materialScope(this, requestParams.deviceConfiguration) {
            when (content) {
                is TileContent.NextSession -> primaryLayout(
                    titleSlot = { text(getString(R.string.tile_label).layoutString) },
                    mainSlot = {
                        text(
                            getString(R.string.home_starts_with, content.summary.firstExercise).layoutString,
                            maxLines = MAIN_TEXT_MAX_LINES,
                        )
                    },
                    bottomSlot = { text(sizeText(content.summary).layoutString) },
                    onClick = openApp(),
                )

                TileContent.NoSession -> primaryLayout(
                    mainSlot = {
                        text(getString(R.string.tile_no_session).layoutString, maxLines = MAIN_TEXT_MAX_LINES)
                    },
                    onClick = openApp(),
                )

                TileContent.Unavailable -> primaryLayout(
                    mainSlot = {
                        text(getString(R.string.tile_unavailable).layoutString, maxLines = MAIN_TEXT_MAX_LINES)
                    },
                    onClick = openApp(),
                )
            }
        }

    /** "4 exercises, about 35 min": the same words as the home screen. */
    private fun sizeText(summary: NextSummary): String = resources.getQuantityString(
        R.plurals.home_next_size,
        summary.exerciseCount,
        summary.exerciseCount,
        summary.minutes,
    )

    /** Tapping anywhere on the Tile opens the app, which is where a session starts. */
    private fun openApp(): Clickable = Clickable.Builder()
        .setId(OPEN_APP_CLICK_ID)
        .setOnClick(
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(packageName)
                        .setClassName(MainActivity::class.java.name)
                        .build(),
                )
                .build(),
        )
        .build()
}
