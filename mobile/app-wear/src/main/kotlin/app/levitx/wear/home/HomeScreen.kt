package app.levitx.wear.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.wear.R
import app.levitx.wear.theme.LevitxWearTheme

/** Home destination: connects [HomeViewModel] to [HomeScreen]. */
@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState)
}

/**
 * The watch home screen: the goal and the next gate.
 *
 * ScreenScaffold ties the scroll indicator to the list, and
 * TransformingLazyColumn scales items at the curved screen edges, as the
 * Wear OS Material 3 guidance recommends.
 */
@Composable
fun HomeScreen(uiState: HomeUiState) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            if (uiState is HomeUiState.Unavailable) {
                item { Text(stringResource(R.string.data_unavailable)) }
            }
            if (uiState is HomeUiState.NoGoal) {
                item { Text(stringResource(R.string.home_no_goal)) }
            }
            if (uiState is HomeUiState.Ready) {
                item { ListHeader { Text(uiState.skillName) } }
                val gateTitle = uiState.gateTitle
                if (gateTitle == null) {
                    item { Text(stringResource(R.string.home_path_complete)) }
                } else {
                    item {
                        Text(
                            text = stringResource(R.string.home_gate_label, uiState.gateNumber, uiState.gateCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = LevitxColors.Accent,
                        )
                    }
                    item { Text(text = gateTitle, style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun HomeScreenPreview() {
    val path = SkillCatalogue.muscleUp
    LevitxWearTheme {
        HomeScreen(
            HomeUiState.Ready(
                skillName = path.name,
                gateTitle = path.gates[2].title,
                gateNumber = 3,
                gateCount = path.gates.size,
            ),
        )
    }
}
