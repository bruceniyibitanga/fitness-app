package app.levitx.wear.intervals

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.intervals.IntervalRun
import app.levitx.core.intervals.IntervalTimeline
import app.levitx.design.LevitxColors
import app.levitx.wear.R
import app.levitx.wear.theme.LevitxWearTheme
import app.levitx.wear.ui.CenteredText
import app.levitx.wear.ui.SectionLabel

/** Interval timer destination: connects [IntervalTimerViewModel] to [IntervalTimerScreen]. */
@Composable
fun IntervalTimerRoute(onClose: () -> Unit, viewModel: IntervalTimerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Starting is asynchronous, so the screen can open a moment before the
    // workout is running. It closes only once there was something to show.
    var hasShownWorkout by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state !is IntervalState.Idle) hasShownWorkout = true
        if (state is IntervalState.Idle && hasShownWorkout) onClose()
    }
    // The workout service may only start while the app is on screen.
    val isRunning = state is IntervalState.Running
    LaunchedEffect(isRunning) {
        if (isRunning) viewModel.keepAlive()
    }
    IntervalTimerScreen(
        state = state,
        actions = IntervalActions(
            onTogglePause = viewModel::togglePause,
            onSkip = viewModel::skip,
            onEnd = viewModel::end,
            onDone = viewModel::dismiss,
        ),
    )
}

/**
 * A cardio or mobility workout on the wrist: the step, a big countdown, a
 * ring that drains with it, and what's next. End only shows while paused,
 * so a stray tap mid-burpee can't end the workout.
 *
 * The screen is kept on, as for strength sessions, so the countdown is
 * never hidden behind the watch face.
 */
@Composable
fun IntervalTimerScreen(state: IntervalState, actions: IntervalActions, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().keepScreenOn()) {
        when (state) {
            IntervalState.Idle -> TimerList { item { CenteredText(stringResource(R.string.interval_none)) } }
            is IntervalState.Running -> Running(state.run, actions)
            is IntervalState.Done -> Done(state, actions.onDone)
        }
    }
}

@Composable
private fun Running(run: IntervalRun, actions: IntervalActions) {
    val step = run.current ?: return
    val color = phaseColor(run, step)
    // A ring around the screen edge drains as the step runs out, readable from the corner of an eye.
    CircularProgressIndicator(
        progress = { (run.remaining.toFloat() / step.seconds.coerceAtLeast(1)).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxSize().padding(CircularProgressIndicatorDefaults.FullScreenPadding),
        colors = ProgressIndicatorDefaults.colors(indicatorColor = color, trackColor = LevitxColors.Track),
    )
    TimerList {
        item { SectionLabel(phaseLabel(run, step), color = color) }
        item {
            Text(
                text = moveText(step),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        item {
            Text(
                text = secondsText(run.remaining),
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.numeralLarge,
                color = if (run.paused) LevitxColors.TextFaint else LevitxColors.Text,
                textAlign = TextAlign.Center,
            )
        }
        item { CenteredText(stringResource(R.string.interval_round, step.round, step.rounds)) }
        item { CenteredText(nextText(run), color = LevitxColors.TextMuted) }
        item {
            Button(
                onClick = actions.onTogglePause,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(if (run.paused) R.string.interval_resume else R.string.interval_pause)) },
            )
        }
        item {
            FilledTonalButton(
                onClick = if (run.paused) actions.onEnd else actions.onSkip,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(if (run.paused) R.string.interval_end else R.string.interval_skip)) },
            )
        }
    }
}

@Composable
private fun Done(state: IntervalState.Done, onDone: () -> Unit) {
    TimerList {
        item { SectionLabel(stringResource(R.string.interval_done_label)) }
        item {
            Text(
                text = state.workout.name,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        item { CenteredText(stringResource(R.string.interval_worked, secondsText(state.workedSeconds))) }
        item {
            CenteredText(
                text = stringResource(
                    when (state.save) {
                        IntervalSave.NOTHING -> R.string.interval_nothing
                        IntervalSave.SAVING -> R.string.interval_saving
                        IntervalSave.SAVED -> R.string.interval_saved
                        IntervalSave.FAILED -> R.string.interval_failed
                    },
                ),
                color = LevitxColors.TextMuted,
            )
        }
        item {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.session_done)) },
            )
        }
    }
}

/** The scrolling list the timer is laid out in, as every watch screen is, so nothing is cut off. */
@Composable
private fun TimerList(content: TransformingLazyColumnScope.() -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding, content = content)
    }
}

/**
 * The tap-driven actions on the timer.
 *
 * @property onTogglePause Pauses or resumes.
 * @property onSkip Ends the step under way.
 * @property onEnd Ends the workout, keeping what was done.
 * @property onDone Leaves the done screen.
 */
data class IntervalActions(
    val onTogglePause: () -> Unit,
    val onSkip: () -> Unit,
    val onEnd: () -> Unit,
    val onDone: () -> Unit,
)

private val previewActions = IntervalActions(onTogglePause = {}, onSkip = {}, onEnd = {}, onDone = {})

@WearPreviewDevices
@Composable
private fun IntervalTimerPreview() {
    val tabata = checkNotNull(IntervalCatalogue.byId("tabata"))
    // Into round 3 of burpees: past get-ready and the jumping jacks.
    val run = IntervalRun(IntervalTimeline.of(tabata)).let { start -> (1..12).fold(start) { run, _ -> run.skip() } }
    LevitxWearTheme { IntervalTimerScreen(IntervalState.Running(tabata, run.tick(6)), previewActions) }
}

@WearPreviewDevices
@Composable
private fun IntervalDonePreview() {
    val workout = checkNotNull(IntervalCatalogue.byId("quiet_cardio"))
    LevitxWearTheme { IntervalTimerScreen(IntervalState.Done(workout, 520, IntervalSave.SAVED), previewActions) }
}
