package app.levitx.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.intervals.IntervalPhase
import app.levitx.core.intervals.IntervalRun
import app.levitx.core.intervals.IntervalTimeline
import app.levitx.phone.intervals.IntervalDetailNavigation
import app.levitx.phone.intervals.IntervalDetailScreen
import app.levitx.phone.intervals.IntervalTimerActions
import app.levitx.phone.intervals.IntervalTimerScreen
import app.levitx.phone.intervals.IntervalTimerUiState
import app.levitx.phone.intervals.SaveStatus
import app.levitx.phone.theme.LevitxTheme
import com.android.tools.screenshot.PreviewTest

/*
 * The cardio and mobility screens, rendered by CI so they can be checked
 * against the "Cardio and mobility workout" and "Interval timer" artboards.
 */

private val noDetailNavigation = IntervalDetailNavigation(onBack = {}, onStart = {}, onOpenMove = {})
private val noTimerActions = IntervalTimerActions(onPause = {}, onSkip = {}, onAskToEnd = {}, onEnd = {}, onDone = {})

/** [workoutId]'s run, moved on to the first step in [phase] and [secondsIn] seconds into it. */
private fun runAt(workoutId: String, phase: IntervalPhase, secondsIn: Int): IntervalRun {
    val steps = IntervalTimeline.of(checkNotNull(IntervalCatalogue.byId(workoutId)))
    val index = steps.indexOfFirst { it.phase == phase }
    return IntervalRun(steps).tick(steps.take(index).sumOf { it.seconds } + secondsIn)
}

@PreviewTest
@Preview(name = "31 Cardio workout", widthDp = 390, heightDp = 844)
@Composable
fun CardioWorkoutScreenshot() {
    LevitxTheme { IntervalDetailScreen(IntervalCatalogue.byId("tabata"), noDetailNavigation) }
}

@PreviewTest
@Preview(name = "32 Mobility workout", widthDp = 390, heightDp = 844)
@Composable
fun MobilityWorkoutScreenshot() {
    LevitxTheme { IntervalDetailScreen(IntervalCatalogue.byId("hips_and_hamstrings"), noDetailNavigation) }
}

@PreviewTest
@Preview(name = "33 Interval timer, work", widthDp = 390, heightDp = 844)
@Composable
fun IntervalWorkScreenshot() {
    val workout = checkNotNull(IntervalCatalogue.byId("tabata"))
    LevitxTheme {
        IntervalTimerScreen(
            IntervalTimerUiState.Running(workout, runAt("tabata", IntervalPhase.WORK, secondsIn = 6)),
            noTimerActions,
        )
    }
}

@PreviewTest
@Preview(name = "34 Interval timer, rest", widthDp = 390, heightDp = 844)
@Composable
fun IntervalRestScreenshot() {
    val workout = checkNotNull(IntervalCatalogue.byId("tabata"))
    LevitxTheme {
        IntervalTimerScreen(
            IntervalTimerUiState.Running(workout, runAt("tabata", IntervalPhase.REST, secondsIn = 3)),
            noTimerActions,
        )
    }
}

@PreviewTest
@Preview(name = "35 Interval timer, switch sides", widthDp = 390, heightDp = 844)
@Composable
fun IntervalSwitchScreenshot() {
    val workout = checkNotNull(IntervalCatalogue.byId("hips_and_hamstrings"))
    LevitxTheme {
        IntervalTimerScreen(
            IntervalTimerUiState.Running(workout, runAt("hips_and_hamstrings", IntervalPhase.SWITCH, secondsIn = 1)),
            noTimerActions,
        )
    }
}

@PreviewTest
@Preview(name = "36 Interval timer, paused and ending", widthDp = 390, heightDp = 844)
@Composable
fun IntervalEndingScreenshot() {
    val workout = checkNotNull(IntervalCatalogue.byId("tabata"))
    val run = runAt("tabata", IntervalPhase.WORK, secondsIn = 12).togglePause()
    LevitxTheme {
        IntervalTimerScreen(IntervalTimerUiState.Running(workout, run, askingToEnd = true), noTimerActions)
    }
}

@PreviewTest
@Preview(name = "37 Interval workout done", widthDp = 390, heightDp = 844)
@Composable
fun IntervalDoneScreenshot() {
    val workout = checkNotNull(IntervalCatalogue.byId("tabata"))
    LevitxTheme {
        IntervalTimerScreen(
            IntervalTimerUiState.Done(workout, workedSeconds = 480, save = SaveStatus.SAVED),
            noTimerActions,
        )
    }
}
