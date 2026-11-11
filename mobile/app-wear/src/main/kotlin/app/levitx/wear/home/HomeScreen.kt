package app.levitx.wear.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import app.levitx.core.load.formatKg
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.wear.R
import app.levitx.wear.theme.LevitxWearTheme
import app.levitx.wear.ui.CenteredText
import app.levitx.wear.ui.SectionLabel

/** Home destination: connects [HomeViewModel] to [HomeScreen]. */
@Composable
fun HomeRoute(onOpenSession: () -> Unit, onChooseLoad: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onStartSession = {
            viewModel.startSession()
            onOpenSession()
        },
        onResumeSession = onOpenSession,
        onChooseLoad = onChooseLoad,
    )
}

/**
 * The watch home screen: the next session the phone's coach planned and the
 * button that starts it.
 *
 * ScreenScaffold ties the scroll indicator to the list, and
 * TransformingLazyColumn scales items at the curved screen edges, as the
 * Wear OS Material 3 guidance recommends.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onStartSession: () -> Unit,
    onResumeSession: () -> Unit,
    onChooseLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState, modifier = modifier) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            when (uiState) {
                HomeUiState.Loading -> Unit
                HomeUiState.Unavailable -> item { CenteredText(stringResource(R.string.data_unavailable)) }
                is HomeUiState.Ready -> readyItems(uiState, onStartSession, onResumeSession, onChooseLoad)
            }
        }
    }
}

private fun TransformingLazyColumnScope.readyItems(
    state: HomeUiState.Ready,
    onStartSession: () -> Unit,
    onResumeSession: () -> Unit,
    onChooseLoad: () -> Unit,
) {
    item { ListHeader { Text(state.goalName ?: stringResource(R.string.home_title)) } }
    val next = state.next
    if (next != null) {
        item { SectionLabel(stringResource(R.string.home_next_label)) }
        item {
            Text(
                text = stringResource(R.string.home_starts_with, next.firstExercise),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        item { CenteredText(nextText(next)) }
    }
    when {
        state.isSessionRunning -> item {
            Button(
                onClick = onResumeSession,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.home_resume_session)) },
            )
        }

        next != null -> item {
            Button(
                onClick = onStartSession,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.home_start_session)) },
            )
        }

        // The plan is cleared when a workout finishes, until the phone has it and plans the next.
        state.waitingToSync > 0 -> item { CenteredText(stringResource(R.string.home_waiting_for_plan)) }

        else -> item { CenteredText(stringResource(R.string.home_no_session)) }
    }
    state.load?.let { load ->
        item {
            FilledTonalButton(
                onClick = onChooseLoad,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.home_load_label)) },
                secondaryLabel = { Text(loadText(load)) },
            )
        }
    }
    if (state.waitingToSync > 0) {
        item {
            CenteredText(
                text = pluralStringResource(R.plurals.home_waiting_to_sync, state.waitingToSync, state.waitingToSync),
                color = LevitxColors.TextMuted,
            )
        }
    }
}

/** "4 exercises, about 35 min". */
@Composable
private fun nextText(next: NextSummary): String =
    pluralStringResource(R.plurals.home_next_size, next.exerciseCount, next.exerciseCount, next.minutes)

/** "Bodyweight only", "+5 kg", "Red band" or "+5 kg, Red band". */
@Composable
private fun loadText(load: LoadSummary): String {
    val added = if (load.addedKg > 0.0) stringResource(R.string.load_added, formatKg(load.addedKg)) else null
    val band = load.bandName?.let { stringResource(R.string.load_band, it) }
    return when {
        added != null && band != null -> stringResource(R.string.load_added_and_band, added, band)
        added != null -> added
        band != null -> band
        else -> stringResource(R.string.load_bodyweight)
    }
}

@WearPreviewDevices
@Composable
private fun HomeScreenPreview() {
    LevitxWearTheme {
        HomeScreen(
            uiState = HomeUiState.Ready(
                goalName = SkillCatalogue.muscleUp.name,
                next = NextSummary(exerciseCount = 4, minutes = 35, firstExercise = "Negative pull-up"),
                load = LoadSummary(addedKg = 0.0, bandName = null),
                waitingToSync = 1,
            ),
            onStartSession = {},
            onResumeSession = {},
            onChooseLoad = {},
        )
    }
}
