package app.levitx.phone.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme

private val ProgressBarHeight = 4.dp

/**
 * Home destination: connects [HomeViewModel] to [HomeScreen].
 *
 * Kept separate from [HomeScreen] so the screen itself takes plain state and
 * can be previewed and tested without a ViewModel.
 */
@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    // Stops collecting while the app is in the background.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState)
}

/** The home screen: which goal the person is working toward and the gate in front of them. */
@Composable
fun HomeScreen(uiState: HomeUiState, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        when (uiState) {
            // Progress is local, so loading takes milliseconds. Showing a
            // spinner for that long would only flicker, so show nothing.
            HomeUiState.Loading -> Unit
            is HomeUiState.Ready -> PathSummary(
                state = uiState,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = LevitxSpacing.Medium, vertical = LevitxSpacing.XLarge),
            )
        }
    }
}

@Composable
private fun PathSummary(state: HomeUiState.Ready, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.home_path_label),
            style = LevitxType.Label,
            color = LevitxColors.TextMuted,
        )
        Spacer(Modifier.height(LevitxSpacing.XSmall))
        Text(text = state.skillName, style = LevitxType.Display)
        Spacer(Modifier.height(LevitxSpacing.Large))

        val gate = state.currentGate
        if (gate == null) {
            Text(
                text = stringResource(R.string.home_path_complete),
                style = MaterialTheme.typography.titleLarge,
            )
        } else {
            Text(
                text = stringResource(R.string.home_gate_label, state.gateNumber, state.gateCount),
                style = LevitxType.Label,
                color = LevitxColors.Accent,
            )
            Spacer(Modifier.height(LevitxSpacing.XSmall))
            Text(text = gate.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(LevitxSpacing.Small))
            Text(
                text = gate.trains,
                style = MaterialTheme.typography.bodyLarge,
                color = LevitxColors.TextSoft,
            )
            Spacer(Modifier.height(LevitxSpacing.Medium))
            GateProgressBar(fraction = state.gateFraction)
        }
    }
}

/**
 * A flat, square-edged progress bar. Screen readers announce it as a
 * progress bar with its percentage.
 */
@Composable
private fun GateProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ProgressBarHeight)
            .background(LevitxColors.Track)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(LevitxColors.Accent),
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    val path = SkillCatalogue.muscleUp
    LevitxTheme {
        HomeScreen(
            HomeUiState.Ready(
                skillName = path.name,
                currentGate = path.gates[2],
                gateNumber = 3,
                gateCount = path.gates.size,
                gateFraction = 0.6f,
            ),
        )
    }
}
