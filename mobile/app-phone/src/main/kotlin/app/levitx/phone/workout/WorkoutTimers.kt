package app.levitx.phone.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.QuietBar
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.SquareNote

/** How often a running timer redraws. A tenth of a second keeps the lowering bar smooth. */
private const val TIMER_TICK_MILLIS = 100L

/** The hold bar runs to half as far again as the target, so going past it shows. */
private const val HOLD_BAR_STRETCH = 1.5f

/** Hold bar divisions, in seconds. */
private const val HOLD_BAR_STEP = 5

/**
 * A hold or slow negatives being timed: a count-in, then the timer, with the
 * whole panel as the stop button. Tapping anywhere is easier than finding a
 * button with chalky hands or a phone on the floor.
 */
@Composable
internal fun ColumnScope.TimerPanel(
    state: WorkoutUiState.Active,
    timer: SetTimer,
    now: () -> Long,
    onTimer: (TimerAction) -> Unit,
) {
    val phase = timer.phaseAt(rememberElapsedNow(now, TIMER_TICK_MILLIS))
    val target = state.nextSet.target
    PlayCue(timerBeat(phase, target), state.soundsOn)
    // Negatives stop by themselves after the last rep, keeping the count.
    val done = phase is TimerPhase.Done
    LaunchedEffect(done) { if (done) onTimer(TimerAction.STOP) }
    val timing = phase !is TimerPhase.LeadIn
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = timing,
                onClickLabel = stringResource(R.string.workout_timer_stop),
                role = Role.Button,
                onClick = { onTimer(TimerAction.STOP) },
            )
            .padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp),
    ) {
        Caption(state.plan.exercise.name)
        when (phase) {
            is TimerPhase.LeadIn -> LeadIn(phase, timer.kind)
            is TimerPhase.Holding -> Hold(phase.seconds, target, state.soundsOn)
            else -> Negatives(phase, timer)
        }
    }
    TimerButtons(timing, timer.kind, timer.countAt(now()), onTimer)
}

/** The 3-2-1 before timing starts, huge, so it reads from the floor. */
@Composable
private fun ColumnScope.LeadIn(phase: TimerPhase.LeadIn, kind: TimerKind) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
    ) {
        Caption(
            stringResource(R.string.workout_timer_get_ready),
            color = LevitxColors.Accent,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            text = phase.secondsLeft.toString(),
            style = LevitxType.Hero.copy(fontSize = 300.sp, lineHeight = 240.sp),
            color = LevitxColors.Accent,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            text = stringResource(
                if (kind == TimerKind.HOLD) R.string.workout_timer_hold_lead else R.string.workout_timer_negative_lead,
            ),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = LevitxColors.TextSoft,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}

/** A hold: seconds over the target, the bar, and a word on form. */
@Composable
private fun Hold(seconds: Int, target: Int, soundsOn: Boolean) {
    val past = seconds >= target
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 18.dp)) {
        Text(
            text = seconds.toString(),
            style = LevitxType.Hero.copy(fontSize = 196.sp, lineHeight = 160.sp),
            color = if (past) LevitxColors.Accent else LevitxColors.Text,
        )
        Text(
            text = stringResource(R.string.workout_timer_of_seconds, target),
            style = LevitxType.Poster,
            color = LevitxColors.TextFaint,
            modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
        )
    }
    // Read out as the hold passes its target, not every second.
    val spoken = stringResource(if (past) R.string.workout_timer_past_spoken else R.string.workout_timer_holding_spoken)
    Caption(
        text = if (past) {
            stringResource(R.string.workout_timer_past, seconds - target)
        } else {
            stringResource(R.string.workout_timer_holding, target - seconds)
        },
        color = if (past) LevitxColors.Accent else LevitxColors.TextMuted,
        modifier = Modifier
            .padding(top = 16.dp)
            .semantics {
                contentDescription = spoken
                liveRegion = LiveRegionMode.Polite
            },
    )
    HoldBar(seconds, target)
    SquareNote(
        title = null,
        text = stringResource(if (past) R.string.workout_timer_hold_past_cue else R.string.workout_timer_hold_cue),
        squareColor = LevitxColors.Ice,
        modifier = Modifier.padding(top = 28.dp),
    )
    Caption(
        text = if (soundsOn) {
            stringResource(R.string.workout_timer_hold_sounds, HOLD_BAR_STEP, target)
        } else {
            stringResource(R.string.workout_timer_sounds_off)
        },
        modifier = Modifier.padding(top = 14.dp),
    )
}

/**
 * Seconds held as a bar: white to the target, orange past it, divided every
 * five seconds, with the target marked.
 */
@Composable
private fun HoldBar(seconds: Int, target: Int) {
    val span = (target * HOLD_BAR_STRETCH).coerceAtLeast(target + 1f)
    val description = pluralStringResource(R.plurals.workout_timer_hold_bar, seconds, seconds, target)
    Canvas(
        modifier = Modifier
            .padding(top = 28.dp)
            .fillMaxWidth()
            .height(32.dp)
            .semantics { contentDescription = description },
    ) {
        val unit = size.width / span
        val top = 6.dp.toPx()
        val barHeight = size.height - 2 * top
        drawRect(LevitxColors.Track, topLeft = Offset(0f, top), size = Size(size.width, barHeight))
        val held = seconds.coerceAtMost(span.toInt()).toFloat()
        drawRect(
            LevitxColors.Text,
            topLeft = Offset(0f, top),
            size = Size(minOf(held, target.toFloat()) * unit, barHeight),
        )
        if (held > target) {
            drawRect(
                LevitxColors.Accent,
                topLeft = Offset(target * unit, top),
                size = Size((held - target) * unit, barHeight),
            )
        }
        for (mark in HOLD_BAR_STEP until span.toInt() step HOLD_BAR_STEP) {
            drawRect(LevitxColors.Background, topLeft = Offset(mark * unit, top), size = Size(1.dp.toPx(), barHeight))
        }
        drawRect(LevitxColors.Accent, topLeft = Offset(target * unit, 0f), size = Size(2.dp.toPx(), size.height))
    }
    Row(modifier = Modifier.padding(top = 8.dp)) {
        Caption("0", color = LevitxColors.TextFaint)
        Spacer(Modifier.weight(1f))
        Caption(stringResource(R.string.workout_timer_target, target), color = LevitxColors.TextFaint)
    }
}

/**
 * Start now and Cancel during the count-in. Once timing: stop and log what
 * was counted, and Restart (a hold) or Cancel (negatives) beside it.
 */
@Composable
private fun TimerButtons(timing: Boolean, kind: TimerKind, count: Int, onTimer: (TimerAction) -> Unit) {
    Row(modifier = Modifier.height(BottomBarHeight)) {
        AccentBar(
            text = when {
                !timing -> stringResource(R.string.workout_timer_start_now)
                kind == TimerKind.HOLD -> stringResource(R.string.workout_timer_stop_seconds, count)
                else -> stringResource(R.string.workout_timer_stop_reps, count)
            },
            hint = stringResource(
                if (timing) R.string.workout_timer_tap_anywhere else R.string.workout_timer_skip_count,
            ),
            onClick = { onTimer(if (timing) TimerAction.STOP else TimerAction.NOW) },
            height = BottomBarHeight,
            modifier = Modifier.weight(2f),
        )
        val restart = timing && kind == TimerKind.HOLD
        QuietBar(
            text = stringResource(if (restart) R.string.workout_timer_restart else R.string.workout_timer_cancel),
            onClick = { onTimer(if (restart) TimerAction.START else TimerAction.CANCEL) },
            modifier = Modifier
                .weight(1f)
                .height(BottomBarHeight),
        )
    }
}
