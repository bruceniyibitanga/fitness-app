package app.levitx.phone.path

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.GateRow
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.QuietBar
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.TabHeader
import app.levitx.phone.ui.gateStatus

/** The bottom bars, as on the artboard. */
private val PlanBarHeight = 64.dp

/**
 * Plan tab: connects [PathViewModel] to [PathScreen].
 *
 * @param onTrain Goes to Today, where the next session trains the current gate.
 * @param onChangeGoal Goes to the programs, where another goal can be picked.
 */
@Composable
fun PathRoute(onTrain: () -> Unit, onChangeGoal: () -> Unit, viewModel: PathViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PathScreen(uiState, onTrain, onChangeGoal)
}

/**
 * The Plan tab, laid out as the "Goal path" artboard: how many gates are left
 * as one big orange number, the gates on a rail, and what the selected gate
 * trains. Train the next gate is pinned to the bottom.
 */
@Composable
fun PathScreen(uiState: PathUiState, onTrain: () -> Unit, onChangeGoal: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(LevitxColors.Background)) {
        TabHeader(stringResource(R.string.tab_plan))
        when (uiState) {
            PathUiState.Loading -> Unit

            PathUiState.Unavailable -> Lede(stringResource(R.string.data_unavailable), Modifier.padding(ScreenPadding))

            PathUiState.NoGoal -> {
                Lede(stringResource(R.string.path_no_goal), Modifier.padding(ScreenPadding).weight(1f))
                AccentBar(
                    text = stringResource(R.string.path_pick_goal),
                    onClick = onChangeGoal,
                    height = PlanBarHeight,
                )
            }

            is PathUiState.Ready -> {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    GoalHeader(uiState)
                    Gates(uiState)
                }
                Row(modifier = Modifier.height(PlanBarHeight)) {
                    AccentBar(
                        text = stringResource(R.string.path_train_next),
                        onClick = onTrain,
                        height = PlanBarHeight,
                        modifier = Modifier.weight(2f),
                    )
                    QuietBar(
                        text = stringResource(R.string.path_change_goal),
                        onClick = onChangeGoal,
                        modifier = Modifier.weight(1f).height(PlanBarHeight),
                    )
                }
            }
        }
    }
}

/** "GOAL · MUSCLE-UP", then the gates left as one big number. */
@Composable
private fun GoalHeader(state: PathUiState.Ready) {
    val gates = state.path.gates.size
    val passed = state.currentIndex ?: gates
    val left = gates - passed
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp, bottom = 20.dp)) {
        Caption(stringResource(R.string.path_goal_label, state.path.name))
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(
                text = left.toString(),
                style = LevitxType.Hero.copy(fontSize = 124.sp, lineHeight = 100.sp),
                color = LevitxColors.Accent,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = if (left == 0) {
                        stringResource(R.string.path_reached)
                    } else {
                        pluralStringResource(R.plurals.path_gates_to_go, left)
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = LevitxColors.Text,
                )
                Text(
                    text = stringResource(R.string.path_passed_count, passed, gates),
                    style = LevitxType.Micro,
                    color = LevitxColors.TextMuted,
                )
            }
        }
    }
    Rule()
}

/** The gates on their rail, and what the selected one trains. */
@Composable
private fun Gates(state: PathUiState.Ready) {
    val gates = state.path.gates
    var selected by rememberSaveable(state.path.id, state.currentIndex) {
        mutableIntStateOf(state.currentIndex ?: gates.lastIndex)
    }
    Column(modifier = Modifier.selectableGroup()) {
        gates.forEachIndexed { index, gate ->
            GateRow(
                gate = gate,
                best = state.bestByGateId[gate.id] ?: 0,
                status = gateStatus(index, state.currentIndex),
                isFirst = index == 0,
                isLast = index == gates.lastIndex,
                selected = index == selected,
                onClick = { selected = index },
            )
            Rule()
        }
    }
    val gate = gates[selected.coerceIn(gates.indices)]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(start = ScreenPadding, end = ScreenPadding, top = 14.dp, bottom = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.path_gate_trains, gate.title).uppercase(),
            style = LevitxType.Micro,
            color = LevitxColors.TextMuted,
        )
        Text(
            text = gate.trains,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = LevitxColors.Text,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
    Rule()
}

@Preview
@Composable
private fun PathScreenPreview() {
    val path = SkillCatalogue.muscleUp
    LevitxTheme {
        PathScreen(
            uiState = PathUiState.Ready(
                path,
                2,
                mapOf(path.gates[0].id to 30, path.gates[1].id to 10, path.gates[2].id to 7),
            ),
            onTrain = {},
            onChangeGoal = {},
        )
    }
}
