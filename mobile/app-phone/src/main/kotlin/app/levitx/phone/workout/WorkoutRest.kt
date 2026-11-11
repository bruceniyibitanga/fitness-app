package app.levitx.phone.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.load.formatKg
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.OutlineBar
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.durationText
import app.levitx.phone.ui.measureText

/** How often the rest countdown redraws. A quarter second keeps the seconds honest. */
private const val COUNTDOWN_TICK_MILLIS = 250L
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60

/** For this long after rest runs out it says "GO"; after that it counts the extra. */
private const val GO_SECONDS = 10

/** A sentence and the way back to Today. */
@Composable
internal fun ColumnScope.Message(text: String, onDone: () -> Unit) {
    Lede(text, modifier = Modifier.padding(ScreenPadding).weight(1f))
    AccentBar(text = stringResource(R.string.workout_back), onClick = onDone, height = BottomBarHeight)
}

/**
 * Rest between sets. It counts down, beeps at zero, and then waits: rest
 * never ends by itself, because the next set starts when the person is ready,
 * and a few extra seconds are fine. The set just logged can be taken back.
 */
@Composable
internal fun ColumnScope.Rest(state: WorkoutUiState.Active, endsAt: Long, now: () -> Long, actions: WorkoutActions) {
    val leftMillis = endsAt - rememberElapsedNow(now, COUNTDOWN_TICK_MILLIS)
    val secondsLeft = ceilSeconds(leftMillis.coerceAtLeast(0))
    val overSeconds = (-leftMillis / MILLIS_PER_SECOND).toInt()
    PlayCue(restBeat(if (leftMillis > 0) secondsLeft else 0), state.soundsOn)
    val plannedSeconds = state.lastSet?.plan?.restSeconds ?: state.plan.restSeconds
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp),
    ) {
        RestClock(secondsLeft, overSeconds, plannedSeconds, running = leftMillis > 0)
        if (leftMillis > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 18.dp)) {
                OutlineBar(stringResource(R.string.workout_rest_shorter), {
                    actions.onRest(RestAction.SHORTER)
                }, Modifier.weight(1f))
                OutlineBar(stringResource(R.string.workout_rest_longer), {
                    actions.onRest(RestAction.LONGER)
                }, Modifier.weight(1f))
            }
        }
        RestNext(state)
        Spacer(Modifier.weight(1f))
        state.lastSet?.let { LastSetRow(it, state.canUndo, onUndo = { actions.onRest(RestAction.UNDO) }) }
    }
    AccentBar(
        text = if (leftMillis > 0) {
            stringResource(R.string.workout_skip_rest)
        } else {
            stringResource(R.string.workout_start_set, state.run.nextSetIndex + 1)
        },
        onClick = { actions.onRest(RestAction.END) },
        height = BottomBarHeight,
    )
}

/** The countdown in ice; "GO" in orange as it runs out; then the extra, calmly. */
@Composable
private fun RestClock(secondsLeft: Int, overSeconds: Int, plannedSeconds: Int, running: Boolean) {
    val (label, labelColor) = when {
        running -> stringResource(R.string.workout_rest_label) to LevitxColors.Ice
        overSeconds < GO_SECONDS -> stringResource(R.string.workout_rest_done) to LevitxColors.Accent
        else -> stringResource(R.string.workout_rest_over) to LevitxColors.TextSoft
    }
    Caption(label, color = labelColor, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    val (digits, digitColor) = when {
        running -> clock(secondsLeft) to LevitxColors.Ice
        overSeconds < GO_SECONDS -> stringResource(R.string.workout_rest_go) to LevitxColors.Accent
        else -> stringResource(R.string.workout_rest_extra, clock(overSeconds)) to LevitxColors.Text
    }
    // The label speaks for the clock; reading every second out would drown everything else.
    Text(
        text = digits,
        style = LevitxType.Hero.copy(fontSize = 168.sp, lineHeight = 136.sp),
        color = digitColor,
        modifier = Modifier
            .padding(top = 18.dp)
            .clearAndSetSemantics { },
    )
    val fraction = if (running) secondsLeft.toFloat() / plannedSeconds.coerceAtLeast(1) else 1f
    Box(
        modifier = Modifier
            .padding(top = 22.dp)
            .fillMaxWidth()
            .height(8.dp)
            .background(LevitxColors.Track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(if (running) LevitxColors.Ice else LevitxColors.Accent),
        )
    }
    Row(modifier = Modifier.padding(top = 8.dp)) {
        Spacer(Modifier.weight(1f))
        Caption(
            stringResource(R.string.workout_rest_planned, durationText(plannedSeconds)),
            color = LevitxColors.TextFaint,
        )
    }
}

/** What comes after the rest: the exercise, the set and its target. */
@Composable
private fun RestNext(state: WorkoutUiState.Active) {
    val plan = state.plan
    val load = loadPerRep(state, state.run.load.addedKg, state.run.load.assistanceKg)
    Rule(Modifier.padding(top = 22.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 16.dp)) {
        Caption(stringResource(R.string.workout_rest_next_set, state.run.nextSetIndex + 1, plan.sets.size))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = plan.exercise.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = LevitxColors.Text,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = measureText(plan.exercise.measure, state.nextSet.target),
                style = LevitxType.Figure,
                color = LevitxColors.Text,
            )
        }
        load?.let { Caption(stringResource(R.string.kg_per_rep, formatKg(it)), color = LevitxColors.TextMuted) }
    }
}

/** "Set 2 logged: 7 reps, 2 left", with Undo when it can still be taken back. */
@Composable
private fun LastSetRow(last: LastSet, canUndo: Boolean, onUndo: () -> Unit) {
    val done = measureText(last.plan.exercise.measure, last.set.value)
    val left = last.set.repsInReserve
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(bottom = 14.dp)
            .fillMaxWidth()
            .height(48.dp)
            .background(LevitxColors.Surface)
            .padding(start = 14.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(LevitxColors.Accent),
        )
        Text(
            text = if (left == null) {
                stringResource(R.string.workout_logged, last.number, done)
            } else {
                stringResource(R.string.workout_logged_left, last.number, done, repsLeftLabel(left))
            },
            fontSize = 14.sp,
            color = LevitxColors.Text,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        )
        if (canUndo) {
            Caption(
                text = stringResource(R.string.workout_undo),
                color = LevitxColors.Accent,
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(role = Role.Button, onClick = onUndo)
                    .padding(horizontal = 16.dp)
                    .wrapContentHeight(Alignment.CenterVertically),
            )
        }
    }
}

/** "1:24": minutes and seconds. */
@Composable
private fun clock(seconds: Int): String =
    stringResource(R.string.clock_format, seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
