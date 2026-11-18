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
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.wear.R
import app.levitx.wear.theme.LevitxWearTheme
import app.levitx.wear.ui.CenteredText
import app.levitx.wear.ui.SectionLabel
import app.levitx.wear.ui.unitLabel
import app.levitx.wear.ui.weightNumber

/** Home destination: connects [HomeViewModel] to [HomeScreen]. */
@Composable
fun HomeRoute(
    onOpenSession: () -> Unit,
    onChooseLoad: () -> Unit,
    onOpenIntervals: () -> Unit,
    onOpenIntervalTimer: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        actions = HomeActions(
            onStartSession = {
                viewModel.startSession()
                onOpenSession()
            },
            onResumeSession = onOpenSession,
            onChooseLoad = onChooseLoad,
            onOpenIntervals = onOpenIntervals,
            onOpenIntervalTimer = onOpenIntervalTimer,
        ),
    )
}

/**
 * What can be tapped on the home screen.
 *
 * @property onStartSession Starts the coach's next session.
 * @property onResumeSession Returns to the session in progress.
 * @property onChooseLoad Opens the load screen.
 * @property onOpenIntervals Opens the cardio and mobility list.
 * @property onOpenIntervalTimer Returns to the cardio or mobility workout in progress.
 */
data class HomeActions(
    val onStartSession: () -> Unit,
    val onResumeSession: () -> Unit,
    val onChooseLoad: () -> Unit,
    val onOpenIntervals: () -> Unit,
    val onOpenIntervalTimer: () -> Unit,
)

/**
 * The watch home screen: the next session the phone's coach planned and the
 * button that starts it.
 *
 * ScreenScaffold ties the scroll indicator to the list, and
 * TransformingLazyColumn scales items at the curved screen edges, as the
 * Wear OS Material 3 guidance recommends.
 */
@Composable
fun HomeScreen(uiState: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState, modifier = modifier) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            when (uiState) {
                HomeUiState.Loading -> Unit
                HomeUiState.Unavailable -> item { CenteredText(stringResource(R.string.data_unavailable)) }
                is HomeUiState.Ready -> readyItems(uiState, actions)
            }
        }
    }
}

private fun TransformingLazyColumnScope.readyItems(state: HomeUiState.Ready, actions: HomeActions) {
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
    mainAction(state, actions)
    state.load?.let { load ->
        item {
            FilledTonalButton(
                onClick = actions.onChooseLoad,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.home_load_label)) },
                secondaryLabel = { Text(loadText(load)) },
            )
        }
    }
    if (!state.isSessionRunning && state.intervalName == null) {
        item {
            FilledTonalButton(
                onClick = actions.onOpenIntervals,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.home_intervals)) },
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

/**
 * The one big button: back to the cardio or mobility workout, back to the
 * session, or start the next one. With none of those it says why.
 */
private fun TransformingLazyColumnScope.mainAction(state: HomeUiState.Ready, actions: HomeActions) {
    when {
        // One workout at a time: while cardio or mobility runs, home only leads back to it.
        state.intervalName != null -> item {
            Button(
                onClick = actions.onOpenIntervalTimer,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.home_back_to, state.intervalName)) },
            )
        }

        state.isSessionRunning -> item {
            Button(
                onClick = actions.onResumeSession,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.home_resume_session)) },
            )
        }

        state.next != null -> item {
            Button(
                onClick = actions.onStartSession,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.home_start_session)) },
            )
        }

        // The plan is cleared when a workout finishes, until the phone has it and plans the next.
        state.isWaitingForPlan -> item { CenteredText(stringResource(R.string.home_waiting_for_plan)) }

        else -> item { CenteredText(stringResource(R.string.home_no_session)) }
    }
}

/** "4 exercises, about 35 min". */
@Composable
private fun nextText(next: NextSummary): String =
    pluralStringResource(R.plurals.home_next_size, next.exerciseCount, next.exerciseCount, next.minutes)

/** "Bodyweight only", "+5 kg", "Red band" or "+5 kg, Red band". */
@Composable
private fun loadText(load: LoadSummary): String {
    val added = load.addedKg.takeIf { it > 0.0 }?.let { kg ->
        stringResource(R.string.load_added, weightNumber(kg), unitLabel())
    }
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
            actions = HomeActions(
                onStartSession = {},
                onResumeSession = {},
                onChooseLoad = {},
                onOpenIntervals = {},
                onOpenIntervalTimer = {},
            ),
        )
    }
}
