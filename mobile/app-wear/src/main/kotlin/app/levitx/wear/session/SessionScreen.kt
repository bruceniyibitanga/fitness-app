package app.levitx.wear.session

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
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
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.wear.R
import app.levitx.wear.theme.LevitxWearTheme
import app.levitx.wear.ui.CenteredText
import app.levitx.wear.ui.SectionLabel
import app.levitx.wear.ui.StepperRow
import app.levitx.wear.ui.measureText
import kotlinx.coroutines.delay

private const val COUNTDOWN_TICK_MILLIS = 250L
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60

/** Largest number the counter goes to. No one does 999 reps in a set. */
private const val MAX_ENTRY = 999

/** Session destination: connects [SessionViewModel] to [SessionScreen]. */
@Composable
fun SessionRoute(onClose: () -> Unit, viewModel: SessionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // With no session there is nothing to show here, so return to home. This
    // is also how the screen closes after "Done" and after cancelling.
    LaunchedEffect(uiState) {
        if (uiState is SessionUiState.NoSession) onClose()
    }
    SessionScreen(
        uiState = uiState,
        nowElapsedMillis = viewModel::elapsedRealtimeMillis,
        onLogSet = viewModel::logSet,
        onEndRest = viewModel::endRest,
        onFinishEarly = viewModel::finishEarly,
        onDone = viewModel::dismissSummary,
    )
}

/**
 * One session on the watch: a counter for reps, a stopwatch for holds, a
 * countdown for rest, then a short summary.
 *
 * The screen is kept on for the session, so a timer is never hidden behind
 * the watch face mid-set.
 *
 * @param nowElapsedMillis The clock the timers run against. Passed in so
 *   previews and tests can control it.
 */
@Composable
fun SessionScreen(
    uiState: SessionUiState,
    nowElapsedMillis: () -> Long,
    onLogSet: (Int) -> Unit,
    onEndRest: () -> Unit,
    onFinishEarly: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().keepScreenOn()) {
        when (uiState) {
            SessionUiState.Loading -> SessionList { item { CircularProgressIndicator() } }
            SessionUiState.Unavailable ->
                SessionList { item { CenteredText(stringResource(R.string.data_unavailable)) } }
            SessionUiState.NoSession ->
                SessionList { item { CenteredText(stringResource(R.string.session_none)) } }
            is SessionUiState.Working -> when (uiState.measure) {
                GateMeasure.REPS -> CountEntry(uiState, uiState.target, onLogSet, onFinishEarly)
                GateMeasure.HOLD_SECONDS -> Hold(uiState, nowElapsedMillis, onLogSet, onFinishEarly)
            }
            is SessionUiState.Resting -> Rest(uiState, nowElapsedMillis, onEndRest, onFinishEarly)
            is SessionUiState.Finished -> Finished(uiState, onDone)
        }
    }
}

/**
 * The scrolling list every phase is laid out in. ScreenScaffold ties the
 * scroll indicator to the list, and TransformingLazyColumn scales items at
 * the curved screen edges, as the Wear OS Material 3 guidance recommends. A
 * list also means nothing is cut off on small watches or at large font sizes.
 */
@Composable
private fun SessionList(content: TransformingLazyColumnScope.() -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding, content = content)
    }
}

/** The counter: starts at [initial], adjusted with minus and plus, then saved. */
@Composable
private fun CountEntry(
    state: SessionUiState.Working,
    initial: Int,
    onLogSet: (Int) -> Unit,
    onFinishEarly: () -> Unit,
) {
    // Starts on the expected number, so hitting the target is a single tap.
    var count by rememberSaveable(state.setNumber, initial) { mutableIntStateOf(initial) }
    SessionList {
        item { SetLabel(state) }
        item {
            StepperRow(
                valueText = count.toString(),
                onDecrease = { count = (count - 1).coerceAtLeast(0) },
                onIncrease = { count = (count + 1).coerceAtMost(MAX_ENTRY) },
                decreaseDescription = stringResource(R.string.decrease_description),
                increaseDescription = stringResource(R.string.increase_description),
                canDecrease = count > 0,
                canIncrease = count < MAX_ENTRY,
            )
        }
        item { CenteredText(setHint(state)) }
        item {
            Button(
                onClick = { onLogSet(count) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.session_log_set)) },
            )
        }
        item { EndButton(state.canSaveEarly, onFinishEarly) }
    }
}

/**
 * A hold: start the stopwatch, hold, stop it. The time then goes to the
 * counter so it can be corrected before saving (stopping a watch one-handed
 * after a hang is rarely exact).
 */
@Composable
private fun Hold(
    state: SessionUiState.Working,
    now: () -> Long,
    onLogSet: (Int) -> Unit,
    onFinishEarly: () -> Unit,
) {
    var startedAtMillis by rememberSaveable(state.setNumber) { mutableStateOf<Long?>(null) }
    var heldSeconds by rememberSaveable(state.setNumber) { mutableStateOf<Int?>(null) }
    val startedAt = startedAtMillis
    val held = heldSeconds
    when {
        held != null -> CountEntry(state, held, onLogSet, onFinishEarly)
        startedAt != null ->
            HoldRunning(state, startedAt, now, onStop = { heldSeconds = secondsSince(startedAt, now()) })
        else -> SessionList {
            item { SetLabel(state) }
            item {
                Text(
                    text = measureText(state.measure, state.target),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.numeralMedium,
                    textAlign = TextAlign.Center,
                )
            }
            item { CenteredText(setHint(state)) }
            item {
                Button(
                    onClick = { startedAtMillis = now() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.session_start_hold)) },
                )
            }
            item { EndButton(state.canSaveEarly, onFinishEarly) }
        }
    }
}

@Composable
private fun HoldRunning(state: SessionUiState.Working, startedAtMillis: Long, now: () -> Long, onStop: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    // Counts against the device's elapsed clock, not by adding up ticks, so it
    // stays right even if the app was in the background for part of the hold.
    val seconds by produceState(initialValue = secondsSince(startedAtMillis, now()), startedAtMillis) {
        while (true) {
            value = secondsSince(startedAtMillis, now())
            delay(COUNTDOWN_TICK_MILLIS)
        }
    }
    // One buzz on reaching the target, so there is no need to watch the screen while hanging.
    LaunchedEffect(startedAtMillis) {
        val untilTarget = state.target * MILLIS_PER_SECOND - (now() - startedAtMillis)
        if (untilTarget > 0) {
            delay(untilTarget)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    SessionList {
        item { SetLabel(state) }
        item {
            Text(
                text = stringResource(R.string.seconds_short, seconds),
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.numeralLarge,
                textAlign = TextAlign.Center,
            )
        }
        item { CenteredText(stringResource(R.string.session_aim, measureText(state.measure, state.target))) }
        item {
            Button(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.session_stop_hold)) },
            )
        }
    }
}

@Composable
private fun Rest(state: SessionUiState.Resting, now: () -> Long, onEndRest: () -> Unit, onFinishEarly: () -> Unit) {
    val remainingMillis = remainingRestMillis(state.restEndsAtElapsedMillis, now, onEndRest)
    val totalSeconds = ((remainingMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    val totalMillis = (state.restSeconds * MILLIS_PER_SECOND).coerceAtLeast(1)

    // A ring around the screen edge drains as rest runs out, readable from the corner of an eye.
    CircularProgressIndicator(
        progress = { (remainingMillis.toFloat() / totalMillis).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxSize().padding(CircularProgressIndicatorDefaults.FullScreenPadding),
        colors = ProgressIndicatorDefaults.colors(indicatorColor = LevitxColors.Ice, trackColor = LevitxColors.Track),
    )
    SessionList {
        item { SectionLabel(stringResource(R.string.session_rest_label), color = LevitxColors.Ice) }
        item {
            Text(
                text = stringResource(
                    R.string.clock_format,
                    totalSeconds / SECONDS_PER_MINUTE,
                    totalSeconds % SECONDS_PER_MINUTE,
                ),
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.numeralLarge,
                textAlign = TextAlign.Center,
            )
        }
        item {
            CenteredText(
                stringResource(
                    R.string.session_rest_next,
                    state.nextSetNumber,
                    state.setCount,
                    measureText(state.measure, state.nextTarget),
                ),
            )
        }
        item {
            Button(
                onClick = onEndRest,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.session_skip_rest)) },
            )
        }
        item { EndButton(canSaveEarly = true, onFinishEarly = onFinishEarly) }
    }
}

/**
 * Milliseconds of rest left, updated a few times a second. Buzzes and calls
 * [onFinished] when it reaches zero.
 */
@Composable
private fun remainingRestMillis(endsAt: Long, now: () -> Long, onFinished: () -> Unit): Long {
    val haptics = LocalHapticFeedback.current
    var remainingMillis by remember(endsAt) { mutableLongStateOf((endsAt - now()).coerceAtLeast(0)) }
    // Counts against the device's elapsed clock, not by adding up ticks, so it
    // stays right even if the app was in the background for part of the rest.
    LaunchedEffect(endsAt) {
        while (true) {
            remainingMillis = (endsAt - now()).coerceAtLeast(0)
            if (remainingMillis == 0L) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onFinished()
                break
            }
            delay(COUNTDOWN_TICK_MILLIS)
        }
    }
    return remainingMillis
}

@Composable
private fun Finished(state: SessionUiState.Finished, onDone: () -> Unit) {
    val best = measureText(state.measure, state.sessionBest)
    SessionList {
        item { SectionLabel(stringResource(R.string.session_summary_label)) }
        item {
            Text(
                text = stringResource(
                    if (state.isNewBest) R.string.session_new_best else R.string.session_best_today,
                    best,
                ),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        if (state.passedGate) {
            item { CenteredText(stringResource(R.string.session_gate_passed, state.gateTitle)) }
        }
        if (state.usedBand) {
            item { CenteredText(stringResource(R.string.band_note)) }
        }
        item { CenteredText(stringResource(R.string.session_sync_note), color = LevitxColors.TextMuted) }
        item {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.session_done)) },
            )
        }
    }
}

@Composable
private fun SetLabel(state: SessionUiState.Working) {
    SectionLabel(stringResource(R.string.session_set_label, state.setNumber, state.setCount))
}

/** One line on how hard to go on this set. */
@Composable
private fun setHint(state: SessionUiState.Working): String = when {
    state.isBestEffort -> stringResource(R.string.session_best_effort_hint)
    state.measure == GateMeasure.HOLD_SECONDS -> stringResource(R.string.session_work_hint_hold)
    else -> pluralStringResource(R.plurals.session_work_hint, state.repsInReserve, state.repsInReserve)
}

/** Ends the session. With nothing logged yet there is nothing to save, so it says "cancel" instead. */
@Composable
private fun EndButton(canSaveEarly: Boolean, onFinishEarly: () -> Unit) {
    FilledTonalButton(
        onClick = onFinishEarly,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(stringResource(if (canSaveEarly) R.string.session_end_early else R.string.session_cancel))
        },
    )
}

private fun secondsSince(startedAtMillis: Long, nowMillis: Long): Int =
    ((nowMillis - startedAtMillis).coerceAtLeast(0) / MILLIS_PER_SECOND).toInt().coerceAtMost(MAX_ENTRY)

@WearPreviewDevices
@Composable
private fun SessionScreenPreview() {
    LevitxWearTheme {
        SessionScreen(
            uiState = SessionUiState.Working(
                setNumber = 2,
                setCount = 4,
                measure = GateMeasure.REPS,
                target = 4,
                isBestEffort = false,
                repsInReserve = 2,
                canSaveEarly = true,
            ),
            nowElapsedMillis = { 0L },
            onLogSet = {},
            onEndRest = {},
            onFinishEarly = {},
            onDone = {},
        )
    }
}
