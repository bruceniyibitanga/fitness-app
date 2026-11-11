package app.levitx.phone.path

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.GateRow
import app.levitx.phone.ui.SectionLabel
import app.levitx.phone.ui.gateStatus

/** Path destination: connects [PathViewModel] to [PathScreen]. */
@Composable
fun PathRoute(viewModel: PathViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PathScreen(uiState)
}

/** Every gate on the path: passed, current or still to come, with the person's best. */
@Composable
fun PathScreen(uiState: PathUiState, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = LevitxSpacing.Medium),
        ) {
            when (uiState) {
                PathUiState.Loading -> Unit

                PathUiState.Unavailable -> item { Text(stringResource(R.string.data_unavailable)) }

                PathUiState.NoGoal -> item {
                    Text(
                        text = stringResource(R.string.path_no_goal),
                        style = MaterialTheme.typography.bodyLarge,
                        color = LevitxColors.TextSoft,
                        modifier = Modifier.padding(vertical = LevitxSpacing.Large),
                    )
                }

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
                            status = gateStatus(index, uiState.currentIndex),
                        )
                        HorizontalDivider(color = LevitxColors.Line)
                    }
                }
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
        )
    }
}
