package app.levitx.wear.session

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import app.levitx.core.skills.GateMeasure
import app.levitx.wear.R
import app.levitx.wear.ui.CenteredText
import app.levitx.wear.ui.SectionLabel
import app.levitx.wear.ui.StepperRow
import app.levitx.wear.ui.measureText
import kotlinx.coroutines.delay

private const val TICK_MILLIS = 250L
private const val MILLIS_PER_SECOND = 1_000L

/** Largest number the counter goes to. No one does 999 reps in a set. */
private const val MAX_ENTRY = 999

/** The counter: starts at [initial], adjusted with minus and plus, then saved. */
@Composable
internal fun CountEntry(
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
        if (state.heartRate != null) item { HeartRateText(state.heartRate) }
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
internal fun Hold(state: SessionUiState.Working, now: () -> Long, onLogSet: (Int) -> Unit, onFinishEarly: () -> Unit) {
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
            if (state.heartRate != null) item { HeartRateText(state.heartRate) }
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
            delay(TICK_MILLIS)
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

private fun secondsSince(startedAtMillis: Long, nowMillis: Long): Int =
    ((nowMillis - startedAtMillis).coerceAtLeast(0) / MILLIS_PER_SECOND).toInt().coerceAtMost(MAX_ENTRY)
