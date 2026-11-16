package app.levitx.phone.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
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
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.StatStrip

/**
 * A gate passed, marked properly before the session carries on: the figure,
 * how far it moved, and what opens next. Passing a gate takes weeks, so it
 * gets more than a line in the summary.
 */
@Composable
internal fun ColumnScope.GatePassed(state: WorkoutUiState.Active, pass: GatePass, onGate: (GateAction) -> Unit) {
    PlayCue(Beat("pass", pass.number, 0, Cue.DONE), state.soundsOn)
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
    ) {
        Headline(pass)
        val load = loadPerRep(state, state.run.load.addedKg, state.run.load.assistanceKg)
        StatStrip(
            figures = listOfNotNull(
                stringResource(R.string.gate_passed_gain, pass.value - pass.was) to
                    stringResource(R.string.gate_passed_gain_label),
                load?.let { formatKg(it) to stringResource(R.string.figure_kg_per_rep) },
                (pass.path.gates.size - pass.number).toString() to stringResource(R.string.gate_passed_left_label),
            ),
            modifier = Modifier.padding(horizontal = ScreenPadding),
        )
        Rule()
        WhatOpens(pass)
    }
    AccentBar(
        text = stringResource(R.string.gate_passed_carry_on),
        onClick = { onGate(GateAction.CARRY_ON) },
        height = BottomBarHeight,
    )
}

/** The orange block: which gate, the figure, and the words. */
@Composable
private fun Headline(pass: GatePass) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LevitxColors.Accent)
            .padding(horizontal = ScreenPadding, vertical = 22.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Caption(
                stringResource(R.string.gate_passed_label, pass.number, pass.path.gates.size),
                color = LevitxColors.Background,
                modifier = Modifier.weight(1f),
            )
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = LevitxColors.Background)
        }
        Text(
            text = pass.value.toString(),
            style = LevitxType.Hero.copy(fontSize = 168.sp, lineHeight = 136.sp),
            color = LevitxColors.Background,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            text = stringResource(R.string.gate_passed_title, pass.gate.title),
            style = LevitxType.Poster,
            color = LevitxColors.Background,
            modifier = Modifier
                .padding(top = 10.dp)
                .semantics { heading() },
        )
    }
}

/** The next gate, and what it trains; or the end of the path. */
@Composable
private fun WhatOpens(pass: GatePass) {
    val next = pass.next
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 18.dp, bottom = 20.dp)) {
        Caption(
            stringResource(if (next == null) R.string.gate_passed_path_done_label else R.string.gate_passed_next_label),
            color = LevitxColors.Ice,
        )
        Text(
            text = next?.title ?: stringResource(R.string.gate_passed_path_done, pass.path.name),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LevitxColors.Text,
            modifier = Modifier.padding(top = 8.dp),
        )
        Lede(
            text = if (next == null) {
                stringResource(R.string.gate_passed_path_done_body)
            } else {
                stringResource(R.string.gate_passed_next_body, next.trains)
            },
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
