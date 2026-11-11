package app.levitx.wear.session

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import app.levitx.design.LevitxColors
import app.levitx.wear.R
import app.levitx.wear.ui.CenteredText
import app.levitx.wear.ui.SectionLabel
import app.levitx.wear.ui.measureText
import kotlinx.coroutines.delay

private const val COUNTDOWN_TICK_MILLIS = 250L
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60

@Composable
internal fun Rest(state: SessionUiState.Resting, now: () -> Long, onEndRest: () -> Unit, onFinishEarly: () -> Unit) {
    val remainingMillis = remainingRestMillis(state.restEndsAtElapsedMillis, now, state.buzzOnScreen, onEndRest)
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
        if (state.heartRate != null) item { HeartRateText(state.heartRate) }
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
 * Milliseconds of rest left, updated a few times a second. Calls [onFinished]
 * when it reaches zero, with a buzz if [buzz] is true. (While the workout
 * service is running it gives the buzz, so the wrist doesn't get two.)
 */
@Composable
private fun remainingRestMillis(endsAt: Long, now: () -> Long, buzz: Boolean, onFinished: () -> Unit): Long {
    val haptics = LocalHapticFeedback.current
    var remainingMillis by remember(endsAt) { mutableLongStateOf((endsAt - now()).coerceAtLeast(0)) }
    // Counts against the device's elapsed clock, not by adding up ticks, so it
    // stays right even if the app was in the background for part of the rest.
    LaunchedEffect(endsAt) {
        while (true) {
            remainingMillis = (endsAt - now()).coerceAtLeast(0)
            if (remainingMillis == 0L) {
                if (buzz) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onFinished()
                break
            }
            delay(COUNTDOWN_TICK_MILLIS)
        }
    }
    return remainingMillis
}

@Composable
internal fun Finished(state: SessionUiState.Finished, onDone: () -> Unit) {
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
