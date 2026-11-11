package app.levitx.wear.load

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import app.levitx.core.load.formatKg
import app.levitx.data.equipment.Band
import app.levitx.design.LevitxColors
import app.levitx.wear.R
import app.levitx.wear.theme.LevitxWearTheme
import app.levitx.wear.ui.CenteredText
import app.levitx.wear.ui.SectionLabel
import app.levitx.wear.ui.StepperRow

/** Load destination: connects [LoadViewModel] to [LoadScreen]. */
@Composable
fun LoadRoute(viewModel: LoadViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoadScreen(
        uiState = uiState,
        onIncreaseAdded = viewModel::increaseAdded,
        onDecreaseAdded = viewModel::decreaseAdded,
        onChooseBand = viewModel::chooseBand,
    )
}

/**
 * Chooses added weight and a band for the next session's skill work. Changes apply as
 * they are made; swiping back returns to home.
 */
@Composable
fun LoadScreen(
    uiState: LoadUiState,
    onIncreaseAdded: () -> Unit,
    onDecreaseAdded: () -> Unit,
    onChooseBand: (Band?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState, modifier = modifier) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text(stringResource(R.string.load_title)) } }
            item { SectionLabel(stringResource(R.string.load_added_label)) }
            item {
                StepperRow(
                    valueText = formatKg(uiState.addedKg),
                    onDecrease = onDecreaseAdded,
                    onIncrease = onIncreaseAdded,
                    decreaseDescription = stringResource(R.string.load_less_description),
                    increaseDescription = stringResource(R.string.load_more_description),
                    canDecrease = uiState.canDecrease,
                    canIncrease = uiState.canIncrease,
                )
            }
            item { SectionLabel(stringResource(R.string.load_band_label)) }
            item {
                RadioButton(
                    selected = uiState.assistanceKg == 0.0,
                    onSelect = { onChooseBand(null) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.load_band_none)) },
                )
            }
            uiState.bands.forEach { band ->
                item {
                    RadioButton(
                        selected = uiState.assistanceKg == band.assistanceKg,
                        onSelect = { onChooseBand(band) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(band.name) },
                        secondaryLabel = {
                            Text(stringResource(R.string.load_band_help, formatKg(band.assistanceKg)))
                        },
                    )
                }
            }
            if (uiState.bands.isEmpty()) {
                item { CenteredText(stringResource(R.string.load_no_bands), color = LevitxColors.TextMuted) }
            }
            item { CenteredText(stringResource(R.string.band_note), color = LevitxColors.TextMuted) }
        }
    }
}

@WearPreviewDevices
@Composable
private fun LoadScreenPreview() {
    LevitxWearTheme {
        LoadScreen(
            uiState = LoadUiState(addedKg = 5.0, assistanceKg = 20.0, bands = listOf(Band("b1", "Red", 20.0))),
            onIncreaseAdded = {},
            onDecreaseAdded = {},
            onChooseBand = {},
        )
    }
}
