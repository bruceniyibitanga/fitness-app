package app.levitx.phone.intervals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.intervals.IntervalPhase
import app.levitx.core.intervals.IntervalRun
import app.levitx.core.intervals.IntervalStep
import app.levitx.core.intervals.IntervalTimeline
import app.levitx.core.intervals.Side
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.BackButton
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.ConfirmSheet
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.QuietBarHeight
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.clockText

/**
 * The interval timer's destination: connects [IntervalTimerViewModel] to [IntervalTimerScreen].
 *
 * @param workoutId The workout's catalogue id from the back stack.
 * @param onDone Leaves the timer.
 */
@Composable
fun IntervalTimerRoute(
    workoutId: String,
    onDone: () -> Unit,
    viewModel: IntervalTimerViewModel = hiltViewModel<IntervalTimerViewModel, IntervalTimerViewModel.Factory>(
        creationCallback = { factory -> factory.create(workoutId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = IntervalTimerActions(
        onPause = viewModel::togglePause,
        onSkip = viewModel::skip,
        onAskToEnd = viewModel::askToEnd,
        onEnd = viewModel::end,
        onDone = onDone,
    )
    // Back mid-workout asks first, so a stray swipe doesn't end it.
    BackHandler(enabled = uiState is IntervalTimerUiState.Running) { actions.onAskToEnd(true) }
    IntervalTimerScreen(uiState, actions)
}

/**
 * What the timer's controls do.
 *
 * @property onPause Pauses or resumes.
 * @property onSkip Ends the step under way.
 * @property onAskToEnd Shows (true) or hides (false) the "End now?" sheet.
 * @property onEnd Ends the workout, keeping what was done.
 * @property onDone Leaves once it's over.
 */
data class IntervalTimerActions(
    val onPause: () -> Unit,
    val onSkip: () -> Unit,
    val onAskToEnd: (Boolean) -> Unit,
    val onEnd: () -> Unit,
    val onDone: () -> Unit,
)

private val ControlsHeight = 64.dp
private val PhaseBarHeight = 8.dp
private val NextRowHeight = 56.dp

/**
 * The interval timer, laid out as the "Interval timer" artboard: the phase,
 * the move, the round, the seconds left in huge type, a bar filling in the
 * phase's colour, what's next, and Pause and Skip at the bottom. The screen
 * stays on while it runs.
 */
@Composable
fun IntervalTimerScreen(uiState: IntervalTimerUiState, actions: IntervalTimerActions, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background)
            .keepScreenOn(),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            when (uiState) {
                IntervalTimerUiState.NotFound -> {
                    BackButton(actions.onDone, modifier = Modifier.padding(4.dp))
                    Lede(stringResource(R.string.interval_not_found), modifier = Modifier.padding(ScreenPadding))
                }

                is IntervalTimerUiState.Running -> RunningBody(uiState, actions)

                is IntervalTimerUiState.Done -> IntervalDone(uiState, actions.onDone)
            }
        }
        if (uiState is IntervalTimerUiState.Running && uiState.askingToEnd) {
            ConfirmSheet(
                title = stringResource(R.string.interval_end_title),
                body = stringResource(R.string.interval_end_body),
                keepLabel = stringResource(R.string.interval_end_keep),
                confirmLabel = stringResource(R.string.interval_end_confirm),
                onKeep = { actions.onAskToEnd(false) },
                onConfirm = actions.onEnd,
            )
        }
    }
}

/** The header, the step under way, what's next, and the controls. */
@Composable
private fun ColumnScope.RunningBody(state: IntervalTimerUiState.Running, actions: IntervalTimerActions) {
    val run = state.run
    val step = run.current ?: return
    Header(state.workout.name, run.totalRemaining, onEnd = { actions.onAskToEnd(true) })
    Column(modifier = Modifier.weight(1f).padding(horizontal = ScreenPadding).padding(top = 22.dp)) {
        val color = phaseColor(step.phase, run.paused)
        Caption(
            text = phaseLabel(step.phase, run.paused),
            color = color,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            text = step.move.name,
            style = LevitxType.Title.copy(fontSize = 42.sp, lineHeight = 41.sp),
            color = LevitxColors.Text,
            modifier = Modifier.padding(top = 12.dp).semantics { heading() },
        )
        Caption(roundText(step), modifier = Modifier.padding(top = 10.dp))
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.weight(1f).fillMaxWidth()) {
            Text(
                text = run.remaining.toString(),
                style = LevitxType.Hero.copy(fontSize = 260.sp, lineHeight = 204.sp),
                color = digitColor(step.phase, run.paused),
            )
        }
        PhaseBar(run.remaining, step.seconds, color)
        NextRow(run)
    }
    Controls(run.paused, actions)
}

/** End on the left, the workout's name in the middle, time left on the right. */
@Composable
private fun Header(name: String, secondsLeft: Int, onEnd: () -> Unit) {
    val endLabel = stringResource(R.string.interval_end)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(QuietBarHeight)
                .clickable(role = Role.Button, onClick = onEnd)
                .semantics { contentDescription = endLabel },
        ) { Icon(painterResource(R.drawable.ic_leave), contentDescription = null, tint = LevitxColors.Text) }
        Caption(name, color = LevitxColors.Text, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        Caption(
            stringResource(R.string.interval_left, clockText(secondsLeft)),
            modifier = Modifier.padding(end = ScreenPadding),
        )
    }
    Rule()
}

/** A thin bar that fills as the step runs out. */
@Composable
private fun PhaseBar(remaining: Int, seconds: Int, color: Color) {
    val label = pluralStringResource(R.plurals.interval_time_left, remaining, remaining, seconds)
    val done = if (seconds == 0) 1f else (seconds - remaining).toFloat() / seconds
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PhaseBarHeight)
            .background(LevitxColors.Track)
            .semantics { contentDescription = label },
    ) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(done.coerceIn(0f, 1f)).background(color))
    }
}

/** "NEXT" and what comes after this step. */
@Composable
private fun NextRow(run: IntervalRun) {
    val next = run.steps.getOrNull(run.index + 1)
    val text = when (next?.phase) {
        null -> stringResource(R.string.interval_next_none)
        IntervalPhase.REST -> stringResource(R.string.interval_next_rest, next.seconds)
        IntervalPhase.SWITCH -> stringResource(R.string.interval_next_switch)
        IntervalPhase.PREPARE, IntervalPhase.WORK -> next.move.name
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(NextRowHeight).padding(top = 14.dp),
    ) {
        Caption(stringResource(R.string.interval_next), color = LevitxColors.TextFaint, modifier = Modifier.weight(1f))
        Text(text, fontSize = 15.sp, color = LevitxColors.Text, textAlign = TextAlign.End)
    }
}

/** Pause or Resume, and Skip. */
@Composable
private fun Controls(paused: Boolean, actions: IntervalTimerActions) {
    Rule()
    Row(modifier = Modifier.fillMaxWidth().height(ControlsHeight)) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
                .background(LevitxColors.Accent)
                .clickable(role = Role.Button, onClick = actions.onPause)
                .padding(horizontal = ScreenPadding),
        ) {
            Text(
                text = stringResource(if (paused) R.string.interval_resume else R.string.interval_pause),
                color = LevitxColors.Background,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(LevitxColors.Surface)
                .clickable(role = Role.Button, onClick = actions.onSkip),
        ) { Text(stringResource(R.string.interval_skip), color = LevitxColors.Text, fontSize = 15.sp) }
    }
}

@Preview
@Composable
private fun IntervalTimerScreenPreview() {
    val workout = checkNotNull(IntervalCatalogue.byId("tabata"))
    val run = IntervalRun(IntervalTimeline.of(workout)).tick(IntervalTimeline.PREPARE_SECONDS + 6)
    LevitxTheme {
        IntervalTimerScreen(
            uiState = IntervalTimerUiState.Running(workout, run),
            actions = IntervalTimerActions(onPause = {}, onSkip = {}, onAskToEnd = {}, onEnd = {}, onDone = {}),
        )
    }
}

/** Shown in the round line: which round, and which side when it matters. */
@Composable
private fun roundText(step: IntervalStep): String {
    val round = stringResource(R.string.interval_round, step.round, step.rounds)
    val side = step.side?.let {
        stringResource(
            if (it == Side.LEFT) R.string.interval_side_left else R.string.interval_side_right,
        )
    }
    return listOfNotNull(round, side).joinToString(" · ")
}
