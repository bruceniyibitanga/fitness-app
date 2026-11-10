package app.levitx.phone.startingtest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.skills.GateMeasure
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.NumberInput
import app.levitx.phone.ui.PrimaryButton
import app.levitx.phone.ui.SecondaryButton
import app.levitx.phone.ui.SectionLabel
import app.levitx.phone.ui.measureText

/** Starting test destination: connects [StartingTestViewModel] to [StartingTestScreen]. */
@Composable
fun StartingTestRoute(onFinished: () -> Unit, viewModel: StartingTestViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StartingTestScreen(
        uiState = uiState,
        onSubmit = viewModel::submit,
        onCantDoIt = viewModel::cantDoItYet,
        onFinished = onFinished,
    )
}

/** One gate at a time: what to do, then how many reps or seconds the person managed. */
@Composable
fun StartingTestScreen(
    uiState: StartingTestUiState,
    onSubmit: (Int) -> Unit,
    onCantDoIt: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Medium),
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LevitxSpacing.Medium, vertical = LevitxSpacing.XLarge),
        ) {
            when (uiState) {
                StartingTestUiState.Loading -> Unit
                StartingTestUiState.Unavailable -> Text(stringResource(R.string.data_unavailable))
                is StartingTestUiState.Testing -> TestingGate(uiState, onSubmit, onCantDoIt)
                is StartingTestUiState.Done -> TestDone(uiState, onFinished)
            }
        }
    }
}

@Composable
private fun TestingGate(state: StartingTestUiState.Testing, onSubmit: (Int) -> Unit, onCantDoIt: () -> Unit) {
    // Keyed on the gate so each gate starts from zero.
    var result by rememberSaveable(state.gate.id) { mutableIntStateOf(0) }
    SectionLabel(stringResource(R.string.test_label, state.gateIndex + 1, state.path.gates.size))
    Text(text = state.gate.title, style = LevitxType.Display)
    if (state.gateIndex == 0) {
        Text(
            text = stringResource(R.string.test_safety),
            style = MaterialTheme.typography.bodyLarge,
            color = LevitxColors.Ice,
        )
    }
    Text(
        text = stringResource(
            if (state.gate.measure == GateMeasure.REPS) R.string.test_instructions_reps else R.string.test_instructions_hold,
        ),
        style = MaterialTheme.typography.bodyLarge,
        color = LevitxColors.TextSoft,
    )
    NumberInput(
        value = result,
        onValueChange = { result = it },
        label = stringResource(
            if (state.gate.measure == GateMeasure.REPS) R.string.input_reps_done else R.string.input_seconds_held,
        ),
    )
    Spacer(Modifier.height(LevitxSpacing.Small))
    PrimaryButton(text = stringResource(R.string.test_save), onClick = { onSubmit(result) })
    SecondaryButton(text = stringResource(R.string.test_cant_do_it), onClick = onCantDoIt)
}

@Composable
private fun TestDone(state: StartingTestUiState.Done, onFinished: () -> Unit) {
    SectionLabel(stringResource(R.string.test_done_label))
    val gate = state.startingGate
    if (gate == null) {
        Text(stringResource(R.string.test_done_all_passed), style = LevitxType.Display)
    } else {
        Text(text = gate.title, style = LevitxType.Display)
        Text(
            text = stringResource(
                R.string.test_done_body,
                state.path.gates.indexOf(gate) + 1,
                measureText(gate.measure, gate.target),
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = LevitxColors.TextSoft,
        )
    }
    Spacer(Modifier.height(LevitxSpacing.Small))
    PrimaryButton(text = stringResource(R.string.test_done_continue), onClick = onFinished)
}

@Preview
@Composable
private fun StartingTestScreenPreview() {
    val path = SkillCatalogue.firstPullUp
    LevitxTheme {
        StartingTestScreen(
            uiState = StartingTestUiState.Testing(path, 0, path.gates.first()),
            onSubmit = {},
            onCantDoIt = {},
            onFinished = {},
        )
    }
}
