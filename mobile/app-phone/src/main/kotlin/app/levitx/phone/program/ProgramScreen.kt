package app.levitx.phone.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import app.levitx.phone.ui.PrimaryButton
import app.levitx.phone.ui.SectionLabel
import app.levitx.phone.ui.gateStatus
import kotlinx.coroutines.launch

/**
 * A program's destination: connects [ProgramViewModel] to [ProgramScreen].
 *
 * @param pathId The skill path's id from the back stack.
 * @param onBack Leaves the screen.
 */
@Composable
fun ProgramRoute(
    pathId: String,
    onBack: () -> Unit,
    viewModel: ProgramViewModel = hiltViewModel<ProgramViewModel, ProgramViewModel.Factory>(
        creationCallback = { factory -> factory.create(pathId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    ProgramScreen(uiState = uiState, onMakePlan = { scope.launch { viewModel.makePlan() } }, onBack = onBack)
}

/** A skill program's gates, the person's progress through them, and the choice to make it the plan. */
@Composable
fun ProgramScreen(uiState: ProgramUiState, onMakePlan: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val ready = uiState as? ProgramUiState.Ready
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (ready != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Small),
                    modifier = Modifier.padding(LevitxSpacing.Medium),
                ) {
                    if (ready.isPlan) {
                        Text(stringResource(R.string.program_is_plan), style = MaterialTheme.typography.titleMedium)
                    } else {
                        Text(
                            text = stringResource(R.string.program_switch_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LevitxColors.TextMuted,
                        )
                        PrimaryButton(text = stringResource(R.string.program_make_plan), onClick = onMakePlan)
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Small),
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LevitxSpacing.Medium),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            when (uiState) {
                ProgramUiState.Loading -> Unit
                ProgramUiState.Unavailable -> Text(stringResource(R.string.data_unavailable))
                ProgramUiState.NotFound -> Text(stringResource(R.string.program_not_found))
                is ProgramUiState.Ready -> Program(uiState)
            }
        }
    }
}

@Composable
private fun Program(state: ProgramUiState.Ready) {
    SectionLabel(stringResource(R.string.program_label), color = LevitxColors.Accent)
    Text(state.path.name, style = LevitxType.Display, modifier = Modifier.semantics { heading() })
    Text(
        text = stringResource(R.string.program_body),
        style = MaterialTheme.typography.bodyLarge,
        color = LevitxColors.TextSoft,
    )
    SectionLabel(stringResource(R.string.program_gates), modifier = Modifier.padding(top = LevitxSpacing.Medium))
    HorizontalDivider(color = LevitxColors.Line)
    state.path.gates.forEachIndexed { index, gate ->
        GateRow(
            number = index + 1,
            gate = gate,
            best = state.bestByGateId[gate.id] ?: 0,
            status = gateStatus(index, state.currentIndex),
        )
        HorizontalDivider(color = LevitxColors.Line)
    }
}

@Preview
@Composable
private fun ProgramScreenPreview() {
    val path = SkillCatalogue.muscleUp
    LevitxTheme {
        ProgramScreen(
            uiState = ProgramUiState.Ready(path, mapOf(path.gates[0].id to 30), currentIndex = 1, isPlan = false),
            onMakePlan = {},
            onBack = {},
        )
    }
}
