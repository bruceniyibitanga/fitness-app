package app.levitx.phone.path

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.skills.Gate
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.SectionLabel
import app.levitx.phone.ui.measureText

private val GateNumberWidth = 40.dp

/** Path destination: connects [PathViewModel] to [PathScreen]. */
@Composable
fun PathRoute(onBack: () -> Unit, viewModel: PathViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PathScreen(uiState, onBack)
}

/** Every gate on the path: passed, current or still to come, with the person's best. */
@Composable
fun PathScreen(uiState: PathUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = LevitxSpacing.Medium),
        ) {
            item { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } }
            when (uiState) {
                PathUiState.Loading -> Unit

                PathUiState.Unavailable -> item { Text(stringResource(R.string.data_unavailable)) }

                is PathUiState.Ready -> {
                    item {
                        Column(modifier = Modifier.padding(vertical = LevitxSpacing.Medium)) {
                            SectionLabel(stringResource(R.string.path_label))
                            Text(uiState.path.name, style = LevitxType.Display)
                        }
                    }
                    itemsIndexed(uiState.path.gates, key = { _, gate -> gate.id }) { index, gate ->
                        GateRow(
                            number = index + 1,
                            gate = gate,
                            best = uiState.bestByGateId[gate.id] ?: 0,
                            status = when {
                                uiState.currentIndex == null || index < uiState.currentIndex -> GateStatus.PASSED
                                index == uiState.currentIndex -> GateStatus.CURRENT
                                else -> GateStatus.LOCKED
                            },
                        )
                        HorizontalDivider(color = LevitxColors.Line)
                    }
                }
            }
        }
    }
}

private enum class GateStatus { PASSED, CURRENT, LOCKED }

@Composable
private fun GateRow(number: Int, gate: Gate, best: Int, status: GateStatus) {
    val statusText = stringResource(
        when (status) {
            GateStatus.PASSED -> R.string.path_status_passed
            GateStatus.CURRENT -> R.string.path_status_current
            GateStatus.LOCKED -> R.string.path_status_locked
        },
    )
    val titleColor = if (status == GateStatus.LOCKED) LevitxColors.TextMuted else LevitxColors.Text
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LevitxSpacing.Medium)
            .semantics(mergeDescendants = true) { stateDescription = statusText },
    ) {
        Text(
            text = number.toString().padStart(2, '0'),
            style = LevitxType.Label,
            color = if (status == GateStatus.CURRENT) LevitxColors.Accent else LevitxColors.TextFaint,
            modifier = Modifier.width(GateNumberWidth),
        )
        Column(verticalArrangement = Arrangement.spacedBy(LevitxSpacing.XSmall)) {
            Text(gate.title, style = MaterialTheme.typography.titleMedium, color = titleColor)
            SectionLabel(
                text = statusText,
                color = if (status == GateStatus.CURRENT) LevitxColors.Accent else LevitxColors.TextMuted,
            )
            if (status == GateStatus.CURRENT) {
                Text(gate.trains, style = MaterialTheme.typography.bodyMedium, color = LevitxColors.TextSoft)
            }
            if (best > 0) {
                Text(
                    text = stringResource(R.string.path_best, measureText(gate.measure, best)),
                    style = MaterialTheme.typography.bodySmall,
                    color = LevitxColors.TextMuted,
                )
            }
        }
    }
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
                mapOf(
                    path.gates[0].id to 30,
                    path.gates[1].id to 10,
                    path.gates[2].id to 7,
                ),
            ),
            onBack = {},
        )
    }
}
