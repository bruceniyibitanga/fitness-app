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
import app.levitx.core.session.trainingDay
import app.levitx.core.skills.GateMeasure
import app.levitx.data.DataUnavailableException
import app.levitx.data.goal.GoalRepository
import app.levitx.data.progress.ProgressRepository
import app.levitx.wear.MainActivity
import app.levitx.wear.R
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future

/** The Tile uses no images, so its resources never change. */
private const val RESOURCES_VERSION = "1"

/**
 * How long the system may show a Tile before asking for a new one. Bests
 * change a few times a week, so a quarter of an hour is fresh enough and
 * costs almost nothing.
 */
private const val FRESHNESS_MILLIS = 15 * 60 * 1_000L

private const val OPEN_APP_CLICK_ID = "open_app"

/** Gate titles and messages may wrap, but not push the rest of the Tile off the screen. */
private const val MAIN_TEXT_MAX_LINES = 3

/**
 * The "Next session" Tile: the current gate and today's sets, one swipe from
 * the watch face. Tapping it opens the app.
 *
 * It reads the same goal and bests as the home screen and builds the same
 * plan from them. It shows training targets only, never results or anything
 * about the person's health, because a Tile is visible to anyone who can see
 * the watch.
 */
@AndroidEntryPoint
class NextSessionTileService : TileService() {
    /** The active goal. Set by Hilt. */
    @Inject
    lateinit var goalRepository: GoalRepository

    /** The person's bests. Set by Hilt. */
    @Inject
    lateinit var progressRepository: ProgressRepository

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
        val day = combine(goalRepository.observeActiveGoal(), progressRepository.observeBests()) { path, bests ->
            path?.trainingDay(bests)
        }.first()
        tileContentFor(day)
    } catch (error: DataUnavailableException) {
        TileContent.Unavailable
    }

    private fun layout(requestParams: RequestBuilders.TileRequest, content: TileContent): LayoutElement =
        materialScope(this, requestParams.deviceConfiguration) {
            when (content) {
                is TileContent.NextSession -> primaryLayout(
                    titleSlot = {
                        text(getString(R.string.home_gate_label, content.gateNumber, content.gateCount).layoutString)
                    },
                    mainSlot = { text(content.gateTitle.layoutString, maxLines = MAIN_TEXT_MAX_LINES) },
                    bottomSlot = { text(todayText(content).layoutString) },
                    onClick = openApp(),
                )

                is TileContent.PathComplete -> primaryLayout(
                    titleSlot = { text(content.skillName.layoutString) },
                    mainSlot = { text(getString(R.string.tile_path_complete).layoutString) },
                    onClick = openApp(),
                )

                TileContent.NoGoal -> primaryLayout(
                    mainSlot = {
                        text(getString(R.string.tile_no_goal).layoutString, maxLines = MAIN_TEXT_MAX_LINES)
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

    /** "3 sets of 4 reps, then a best effort": the same words as the home screen. */
    private fun todayText(content: TileContent.NextSession): String {
        val target = when (content.measure) {
            GateMeasure.REPS -> resources.getQuantityString(R.plurals.reps, content.workTarget, content.workTarget)
            GateMeasure.HOLD_SECONDS -> getString(R.string.seconds_short, content.workTarget)
        }
        return resources.getQuantityString(R.plurals.home_today, content.workSets, content.workSets, target)
    }

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
