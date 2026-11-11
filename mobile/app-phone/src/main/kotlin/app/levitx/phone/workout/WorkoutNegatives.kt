package app.levitx.phone.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.SquareNote
import app.levitx.phone.ui.measureText

/** More reps than this and the rep squares would be too thin to read, so they show as a count. */
private const val MAX_REP_SQUARES = 10

/**
 * Slow negatives, paced: back to the top, then a timed lowering, rep after
 * rep. The big number counts the seconds down; the squares show the reps.
 * Nobody can tap a phone while hanging from a bar, so beeps set the pace and
 * nothing needs touching until the set is over.
 */
@Composable
internal fun ColumnScope.Negatives(phase: TimerPhase, timer: SetTimer) {
    val lowerSeconds = ceilSeconds(timer.lowerMillis)
    val rep = when (phase) {
        is TimerPhase.ToTop -> phase.rep
        is TimerPhase.Lowering -> phase.rep
        else -> timer.reps
    }
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 18.dp)) {
        NegativeClock(phase, lowerSeconds, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 6.dp)) {
            Text(
                text = stringResource(R.string.workout_of_count, rep, timer.reps),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = LevitxColors.Text,
            )
            Caption(stringResource(R.string.workout_timer_reps))
        }
    }
    val status = when (phase) {
        is TimerPhase.ToTop -> stringResource(R.string.workout_timer_to_top, phase.rep)
        is TimerPhase.Lowering -> stringResource(R.string.workout_timer_lowering)
        else -> stringResource(R.string.workout_timer_all_paced)
    }
    Caption(
        text = status,
        color = if (phase is TimerPhase.Lowering) LevitxColors.Accent else LevitxColors.Ice,
        modifier = Modifier
            .padding(top = 16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
    TempoStrip(phase, timer, lowerSeconds)
    RepSquares(rep, timer.reps, done = phase is TimerPhase.Done)
    Spacer(Modifier.weight(1f))
    SquareNote(
        title = null,
        text = stringResource(R.string.workout_timer_negative_cue),
        squareColor = LevitxColors.Ice,
        modifier = Modifier.padding(bottom = 18.dp),
    )
}

/** Seconds to the next lowering in ice, then the lowering's seconds in orange, over its length. */
@Composable
private fun NegativeClock(phase: TimerPhase, lowerSeconds: Int, modifier: Modifier = Modifier) {
    val (figure, color) = when (phase) {
        is TimerPhase.ToTop -> phase.secondsLeft to LevitxColors.Ice
        is TimerPhase.Lowering -> ceilSeconds(phase.millisIn).coerceAtLeast(1) to LevitxColors.Accent
        else -> lowerSeconds to LevitxColors.Text
    }
    // The figure changes every second, so it is left out of what screen readers hear; the status line speaks.
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier.clearAndSetSemantics { }) {
        Text(
            text = figure.toString(),
            style = LevitxType.Hero.copy(fontSize = 196.sp, lineHeight = 160.sp),
            color = color,
        )
        if (phase is TimerPhase.Lowering) {
            Text(
                text = stringResource(R.string.workout_timer_of_seconds, lowerSeconds),
                style = LevitxType.Poster,
                color = LevitxColors.TextFaint,
                modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
            )
        }
    }
}

/** One rep's tempo: back to the top, then the lowering, filling as it goes. */
@Composable
private fun TempoStrip(phase: TimerPhase, timer: SetTimer, lowerSeconds: Int) {
    val topWeight = TO_TOP_MILLIS.toFloat()
    val lowerWeight = timer.lowerMillis.toFloat()
    val topDone = when (phase) {
        is TimerPhase.ToTop -> 1f - phase.millisLeft / topWeight
        else -> 1f
    }
    val lowerDone = when (phase) {
        is TimerPhase.Lowering -> phase.millisIn / lowerWeight
        is TimerPhase.Done -> 1f
        else -> 0f
    }
    val description = stringResource(
        R.string.workout_timer_tempo,
        measureText(GateMeasure.HOLD_SECONDS, ceilSeconds(TO_TOP_MILLIS)),
        measureText(GateMeasure.HOLD_SECONDS, lowerSeconds),
    )
    Canvas(
        modifier = Modifier
            .padding(top = 28.dp)
            .fillMaxWidth()
            .height(20.dp)
            .semantics { contentDescription = description },
    ) {
        val gap = 4.dp.toPx()
        val topWidth = (size.width - gap) * topWeight / (topWeight + lowerWeight)
        val lowerStart = topWidth + gap
        val lowerWidth = size.width - lowerStart
        drawRect(LevitxColors.Track, size = Size(topWidth, size.height))
        drawRect(LevitxColors.Ice, size = Size(topWidth * topDone, size.height))
        drawRect(LevitxColors.Track, topLeft = Offset(lowerStart, 0f), size = Size(lowerWidth, size.height))
        drawRect(
            LevitxColors.Accent,
            topLeft = Offset(lowerStart, 0f),
            size = Size(lowerWidth * lowerDone, size.height),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
        Caption(
            stringResource(R.string.workout_timer_tempo_top, ceilSeconds(TO_TOP_MILLIS)),
            color = LevitxColors.TextFaint,
            modifier = Modifier.weight(topWeight),
        )
        Caption(
            stringResource(R.string.workout_timer_tempo_lower, lowerSeconds),
            color = LevitxColors.Accent,
            modifier = Modifier.weight(lowerWeight),
        )
    }
}

/** A square per rep: done in white, this one outlined orange, the rest outlined faint. */
@Composable
private fun RepSquares(rep: Int, reps: Int, done: Boolean) {
    if (reps > MAX_REP_SQUARES) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(top = 28.dp)
            .fillMaxWidth()
            .clearAndSetSemantics { },
    ) {
        for (number in 1..reps) {
            val isDone = done || number < rep
            val isNow = !done && number == rep
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(if (isDone) LevitxColors.Text else LevitxColors.Background)
                    .border(
                        width = if (isNow) 2.dp else 1.dp,
                        color = when {
                            isNow -> LevitxColors.Accent
                            isDone -> LevitxColors.Text
                            else -> LevitxColors.Tick
                        },
                    ),
            ) {
                Text(
                    text = number.toString(),
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) {
                        LevitxColors.Background
                    } else {
                        LevitxColors.Accent.takeIf { isNow }
                            ?: LevitxColors.TextFaint
                    },
                )
            }
        }
    }
}
