package app.levitx.phone.intervals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.levitx.core.intervals.IntervalPhase
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.clockText

/*
 * The end of an interval workout, and how each phase looks on the timer.
 */

private val DoneBarHeight = 64.dp

/** "Done", how much work that was, whether it's in the history, and the way back. */
@Composable
internal fun ColumnScope.IntervalDone(state: IntervalTimerUiState.Done, onDone: () -> Unit) {
    Column(modifier = Modifier.weight(1f).padding(ScreenPadding).padding(top = 40.dp)) {
        Caption(state.workout.name)
        Text(
            text = stringResource(R.string.interval_done),
            style = LevitxType.Poster,
            color = LevitxColors.Text,
            modifier = Modifier.padding(top = 12.dp).semantics { heading() },
        )
        Caption(
            text = stringResource(R.string.interval_done_worked, clockText(state.workedSeconds)),
            color = LevitxColors.Accent,
            modifier = Modifier.padding(top = 14.dp),
        )
        Lede(
            text = stringResource(
                when (state.save) {
                    SaveStatus.NOTHING -> R.string.interval_done_nothing
                    SaveStatus.SAVING -> R.string.interval_done_saving
                    SaveStatus.SAVED -> R.string.interval_done_saved
                    SaveStatus.FAILED -> R.string.interval_done_failed
                },
            ),
            modifier = Modifier.padding(top = 18.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    AccentBar(text = stringResource(R.string.interval_back), onClick = onDone, height = DoneBarHeight)
}

/** "GET READY", "WORK", "REST", "SWITCH SIDES", or "PAUSED". */
@Composable
internal fun phaseLabel(phase: IntervalPhase, paused: Boolean): String = stringResource(
    when {
        paused -> R.string.interval_paused

        else -> when (phase) {
            IntervalPhase.PREPARE -> R.string.interval_phase_prepare
            IntervalPhase.WORK -> R.string.interval_phase_work
            IntervalPhase.REST -> R.string.interval_phase_rest
            IntervalPhase.SWITCH -> R.string.interval_phase_switch
        }
    },
)

/** Orange to get going and work, ice blue to rest, white to switch sides, grey while paused. */
internal fun phaseColor(phase: IntervalPhase, paused: Boolean): Color = when {
    paused -> LevitxColors.TextMuted
    phase == IntervalPhase.REST -> LevitxColors.Ice
    phase == IntervalPhase.SWITCH -> LevitxColors.Text
    else -> LevitxColors.Accent
}

/** The big seconds: faint while paused, ice blue at rest, white otherwise. */
internal fun digitColor(phase: IntervalPhase, paused: Boolean): Color = when {
    paused -> LevitxColors.TextFaint
    phase == IntervalPhase.REST -> LevitxColors.Ice
    else -> LevitxColors.Text
}
