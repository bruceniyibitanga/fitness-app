package app.levitx.phone.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.QuietBar
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.measureText
import kotlinx.coroutines.delay

/** How often the rest countdown redraws. A quarter second keeps the seconds honest. */
private const val COUNTDOWN_TICK_MILLIS = 250L
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60

/** A sentence and the way back to Today. */
@Composable
internal fun ColumnScope.Message(text: String, onDone: () -> Unit) {
    Lede(text, modifier = Modifier.padding(ScreenPadding).weight(1f))
    AccentBar(text = stringResource(R.string.workout_back), onClick = onDone, height = BottomBarHeight)
}

/** Rest between sets: the countdown, huge, and what comes next. */
@Composable
internal fun ColumnScope.Rest(state: WorkoutUiState.Active, endsAt: Long, now: () -> Long, actions: WorkoutActions) {
    val totalSeconds = rememberSecondsLeft(endsAt, now, actions.onEndRest)
    val plan = state.plan
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp),
    ) {
        Caption(stringResource(R.string.workout_rest_label), color = LevitxColors.Ice)
        Text(
            text = stringResource(
                R.string.clock_format,
                totalSeconds / SECONDS_PER_MINUTE,
                totalSeconds % SECONDS_PER_MINUTE,
            ),
            style = LevitxType.Hero.copy(fontSize = 140.sp, lineHeight = 120.sp),
            color = LevitxColors.Ice,
            // A polite live region lets screen readers hear the countdown without
            // it interrupting whatever they are reading.
            modifier = Modifier
                .padding(top = 18.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            text = stringResource(
                R.string.workout_rest_next,
                plan.exercise.name,
                state.run.nextSetIndex + 1,
                plan.sets.size,
                measureText(plan.exercise.measure, state.nextSet.target),
            ),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = LevitxColors.TextSoft,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
    RestButtons(actions)
}

/** Skip rest, the main action, and Finish beside it. */
@Composable
private fun RestButtons(actions: WorkoutActions) {
    Row(modifier = Modifier.height(BottomBarHeight)) {
        AccentBar(
            text = stringResource(R.string.workout_skip_rest),
            onClick = actions.onEndRest,
            height = BottomBarHeight,
            modifier = Modifier.weight(2f),
        )
        QuietBar(
            text = stringResource(R.string.workout_finish_short_sentence),
            onClick = actions.onFinish,
            modifier = Modifier
                .weight(1f)
                .height(BottomBarHeight),
        )
    }
}

/** The workout is saved: how many sets, any new best, and back to Today. */
@Composable
internal fun ColumnScope.Summary(state: WorkoutUiState.Finished, onDone: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .weight(1f)
            .padding(start = ScreenPadding, end = ScreenPadding, top = 40.dp),
    ) {
        Caption(stringResource(R.string.workout_saved_label), color = LevitxColors.Accent)
        Text(
            text = pluralStringResource(R.plurals.workout_saved_sets, state.sets, state.sets),
            style = LevitxType.Poster,
            color = LevitxColors.Text,
            modifier = Modifier.semantics { heading() },
        )
        state.newBest?.let { best ->
            Text(
                text = stringResource(R.string.workout_new_best, best.gateTitle, measureText(best.measure, best.value)),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = LevitxColors.Accent,
            )
        }
        Lede(stringResource(R.string.workout_saved_body))
    }
    AccentBar(text = stringResource(R.string.workout_done), onClick = onDone, height = BottomBarHeight)
}

/**
 * Whole seconds left until [endsAt], updated a few times a second. Calls
 * [onEnd] with a buzz when it reaches zero.
 */
@Composable
private fun rememberSecondsLeft(endsAt: Long, now: () -> Long, onEnd: () -> Unit): Int {
    val haptics = LocalHapticFeedback.current
    var remainingMillis by remember(endsAt) { mutableLongStateOf((endsAt - now()).coerceAtLeast(0)) }
    // Counts against the device's elapsed clock, not by adding up ticks, so it
    // stays right even if the app was in the background for part of the rest.
    LaunchedEffect(endsAt) {
        while (true) {
            remainingMillis = (endsAt - now()).coerceAtLeast(0)
            if (remainingMillis == 0L) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onEnd()
                break
            }
            delay(COUNTDOWN_TICK_MILLIS)
        }
    }
    return ((remainingMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
}
