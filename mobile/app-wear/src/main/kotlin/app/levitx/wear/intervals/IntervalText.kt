package app.levitx.wear.intervals

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.levitx.core.intervals.IntervalPhase
import app.levitx.core.intervals.IntervalRun
import app.levitx.core.intervals.IntervalStep
import app.levitx.core.intervals.Side
import app.levitx.design.LevitxColors
import app.levitx.wear.R

/*
 * What the interval timer says about each step, kept out of the screen so
 * it reads as layout only.
 */

private const val SECONDS_PER_MINUTE = 60

/** "WORK", "REST", "GET READY", "SWITCH SIDES", or "PAUSED" while paused. */
@Composable
internal fun phaseLabel(run: IntervalRun, step: IntervalStep): String = stringResource(
    when {
        run.paused -> R.string.interval_paused

        else -> when (step.phase) {
            IntervalPhase.PREPARE -> R.string.interval_ready
            IntervalPhase.WORK -> R.string.interval_work
            IntervalPhase.SWITCH -> R.string.interval_switch
            IntervalPhase.REST -> R.string.interval_rest
        }
    },
)

/** Orange for work, blue for rest, plain for getting ready and switching, faint while paused. */
internal fun phaseColor(run: IntervalRun, step: IntervalStep): Color = when {
    run.paused -> LevitxColors.TextFaint
    step.phase == IntervalPhase.WORK -> LevitxColors.Accent
    step.phase == IntervalPhase.REST -> LevitxColors.Ice
    else -> LevitxColors.TextSoft
}

/** The move, with its side when it has one: "Pigeon stretch, right". */
@Composable
internal fun moveText(step: IntervalStep): String = when (step.side) {
    Side.LEFT -> stringResource(R.string.interval_left_side, step.move.name)
    Side.RIGHT -> stringResource(R.string.interval_right_side, step.move.name)
    null -> step.move.name
}

/** "14" under a minute, "1:30" from a minute up. */
@Composable
internal fun secondsText(seconds: Int): String = if (seconds < SECONDS_PER_MINUTE) {
    seconds.toString()
} else {
    stringResource(R.string.clock_format, seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
}

/** What comes after this step: "Next: rest, 10 s", "Next: Burpees, 20 s", or "Last step". */
@Composable
internal fun nextText(run: IntervalRun): String {
    val next = run.steps.getOrNull(run.index + 1) ?: return stringResource(R.string.interval_last_step)
    return when (next.phase) {
        IntervalPhase.REST -> stringResource(R.string.interval_next_rest, next.seconds)

        IntervalPhase.SWITCH -> stringResource(R.string.interval_next_switch)

        IntervalPhase.WORK, IntervalPhase.PREPARE -> stringResource(
            R.string.interval_next_work,
            moveText(next),
            next.seconds,
        )
    }
}
