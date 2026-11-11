package app.levitx.wear.session

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.wear.R
import app.levitx.wear.theme.LevitxWearTheme
import app.levitx.wear.ui.CenteredText
import app.levitx.wear.workout.WorkoutPermissions

/*
 * The session screen is split over three files so each stays short:
 * this one (the screen and what every phase shares), SetPhase.kt (doing and
 * logging a set), RestAndSummary.kt (rest and the summary) and
 * TrackingPrompt.kt (asking to read heart rate).
 */

/** Session destination: connects [SessionViewModel] to [SessionScreen]. */
@Composable
fun SessionRoute(onClose: () -> Unit, viewModel: SessionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // With no session there is nothing to show here, so return to home. This
    // is also how the screen closes after "Done" and after cancelling.
    LaunchedEffect(uiState) {
        if (uiState is SessionUiState.NoSession) onClose()
    }
    // The workout service may only be started while the app is on screen, so
    // it is started (or restarted, after the app was killed) from here.
    val isRunning = uiState is SessionUiState.Working || uiState is SessionUiState.Resting
    LaunchedEffect(isRunning) {
        if (isRunning) viewModel.ensureTracking()
    }
    // Whatever the answer, the session carries on; only heart rate depends on it.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.onTrackingAnswered() }
    SessionScreen(
        uiState = uiState,
        nowElapsedMillis = viewModel::elapsedRealtimeMillis,
        onLogSet = viewModel::logSet,
        onEndRest = viewModel::endRest,
        actions = SetActions(onSkipExercise = viewModel::skipExercise, onFinishEarly = viewModel::finishEarly),
        onDone = viewModel::dismissSummary,
        onAllowTracking = { permissionLauncher.launch(WorkoutPermissions.toRequest) },
        onSkipTracking = viewModel::onTrackingAnswered,
    )
}

/**
 * One workout on the watch, exercise by exercise: a counter for reps, a
 * stopwatch for holds, a countdown for rest, then a short summary.
 *
 * The screen is kept on for the workout, so a timer is never hidden behind
 * the watch face mid-set.
 *
 * @param nowElapsedMillis The clock the timers run against. Passed in so
 *   previews and tests can control it.
 */
@Composable
fun SessionScreen(
    uiState: SessionUiState,
    nowElapsedMillis: () -> Long,
    onLogSet: (Int) -> Unit,
    onEndRest: () -> Unit,
    actions: SetActions,
    onDone: () -> Unit,
    onAllowTracking: () -> Unit,
    onSkipTracking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().keepScreenOn()) {
        when (uiState) {
            SessionUiState.Loading -> SessionList { item { CircularProgressIndicator() } }

            SessionUiState.Unavailable ->
                SessionList { item { CenteredText(stringResource(R.string.data_unavailable)) } }

            SessionUiState.NoSession ->
                SessionList { item { CenteredText(stringResource(R.string.session_none)) } }

            SessionUiState.AskTracking -> TrackingPrompt(onAllowTracking, onSkipTracking)

            is SessionUiState.Working -> when (uiState.measure) {
                GateMeasure.REPS -> CountEntry(uiState, uiState.target, onLogSet, actions)
                GateMeasure.HOLD_SECONDS -> Hold(uiState, nowElapsedMillis, onLogSet, actions)
            }

            is SessionUiState.Resting -> Rest(uiState, nowElapsedMillis, onEndRest, actions.onFinishEarly)

            is SessionUiState.Finished -> Finished(uiState, onDone)
        }
    }
}

/**
 * The scrolling list every phase is laid out in. ScreenScaffold ties the
 * scroll indicator to the list, and TransformingLazyColumn scales items at
 * the curved screen edges, as the Wear OS Material 3 guidance recommends. A
 * list also means nothing is cut off on small watches or at large font sizes.
 */
@Composable
internal fun SessionList(content: TransformingLazyColumnScope.() -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding, content = content)
    }
}

/** Ends the workout. With nothing logged yet there is nothing to save, so it says "cancel" instead. */
@Composable
internal fun EndButton(canSaveEarly: Boolean, onFinishEarly: () -> Unit) {
    FilledTonalButton(
        onClick = onFinishEarly,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(stringResource(if (canSaveEarly) R.string.session_end_early else R.string.session_cancel))
        },
    )
}

/** "128 bpm" in the rest colour, or nothing when heart rate isn't being read. */
@Composable
internal fun HeartRateText(heartRate: Int?) {
    if (heartRate != null) {
        CenteredText(stringResource(R.string.session_heart_rate, heartRate), color = LevitxColors.Ice)
    }
}

@WearPreviewDevices
@Composable
private fun SessionScreenPreview() {
    LevitxWearTheme {
        SessionScreen(
            uiState = SessionUiState.Working(
                exerciseName = "Negative pull-up",
                exerciseNumber = 1,
                exerciseCount = 4,
                setNumber = 2,
                setCount = 3,
                measure = GateMeasure.REPS,
                target = 4,
                isPerSide = false,
                isBestEffort = false,
                repsInReserve = 2,
                canSaveEarly = true,
            ),
            nowElapsedMillis = { 0L },
            onLogSet = {},
            onEndRest = {},
            actions = SetActions(onSkipExercise = {}, onFinishEarly = {}),
            onDone = {},
            onAllowTracking = {},
            onSkipTracking = {},
        )
    }
}
