package app.levitx.phone.train

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.WorkoutFocus
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.PrimaryButton
import app.levitx.phone.ui.SectionLabel
import app.levitx.phone.ui.SessionList
import kotlinx.coroutines.launch

/**
 * A single workout's destination: connects [FocusWorkoutViewModel] to [FocusWorkoutScreen].
 *
 * @param focusName The [WorkoutFocus] name from the back stack.
 */
@Composable
fun FocusWorkoutRoute(
    focusName: String,
    navigation: FocusWorkoutNavigation,
    viewModel: FocusWorkoutViewModel = hiltViewModel<FocusWorkoutViewModel, FocusWorkoutViewModel.Factory>(
        creationCallback = { factory -> factory.create(focusName) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    FocusWorkoutScreen(
        uiState = uiState,
        onStart = { scope.launch { if (viewModel.start()) navigation.onOpenWorkout() } },
        navigation = navigation,
    )
}

/**
 * Where a single workout's screen can lead.
 *
 * @property onBack Goes back to the Train tab.
 * @property onOpenWorkout Opens the workout in progress.
 * @property onOpenExercise Opens an exercise's detail by id.
 */
data class FocusWorkoutNavigation(
    val onBack: () -> Unit,
    val onOpenWorkout: () -> Unit,
    val onOpenExercise: (String) -> Unit,
)

/** The workout for one part of the body, as the coach would have the person do it today. */
@Composable
fun FocusWorkoutScreen(
    uiState: FocusWorkoutUiState,
    onStart: () -> Unit,
    navigation: FocusWorkoutNavigation,
    modifier: Modifier = Modifier,
) {
    val ready = uiState as? FocusWorkoutUiState.Ready
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (ready?.session != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Small),
                    modifier = Modifier.padding(LevitxSpacing.Medium),
                ) {
                    if (ready.hasWorkoutInProgress) {
                        Text(
                            text = stringResource(R.string.focus_workout_in_progress),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LevitxColors.TextMuted,
                        )
                    }
                    val label = if (ready.hasWorkoutInProgress) {
                        R.string.focus_workout_resume
                    } else {
                        R.string.focus_workout_start
                    }
                    PrimaryButton(text = stringResource(label), onClick = onStart)
                }
            }
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Medium),
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LevitxSpacing.Medium),
        ) {
            TextButton(onClick = navigation.onBack) { Text(stringResource(R.string.back)) }
            when (uiState) {
                FocusWorkoutUiState.Loading -> Unit
                FocusWorkoutUiState.Unavailable -> Text(stringResource(R.string.data_unavailable))
                FocusWorkoutUiState.NotFound -> Text(stringResource(R.string.focus_workout_not_found))
                is FocusWorkoutUiState.Ready -> Workout(uiState, navigation.onOpenExercise)
            }
        }
    }
}

@Composable
private fun Workout(state: FocusWorkoutUiState.Ready, onOpenExercise: (String) -> Unit) {
    SectionLabel(stringResource(R.string.focus_workout_label), color = LevitxColors.Accent)
    Text(
        text = focusName(state.focus),
        style = LevitxType.Display,
        modifier = Modifier.semantics { heading() },
    )
    val session = state.session
    if (session == null) {
        Text(
            text = stringResource(R.string.train_nothing_safe),
            style = MaterialTheme.typography.bodyLarge,
            color = LevitxColors.TextSoft,
        )
        return
    }
    SectionLabel(
        pluralStringResource(
            R.plurals.train_workout_meta,
            session.plans.size,
            session.estimatedMinutes,
            session.plans.size,
        ),
    )
    SessionList(session, state.bodyweightKg, onExerciseClick = onOpenExercise)
    Text(
        text = stringResource(R.string.focus_workout_note),
        style = MaterialTheme.typography.bodyMedium,
        color = LevitxColors.TextMuted,
    )
}

@Preview
@Composable
private fun FocusWorkoutScreenPreview() {
    val model = AthleteModel.starting(
        CoachProfile("first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR, Equipment.LOW_BAR)),
        PullUpAnswer.NOT_YET,
    )
    LevitxTheme {
        FocusWorkoutScreen(
            uiState = FocusWorkoutUiState.Ready(
                focus = WorkoutFocus.PULL,
                session = Coach().workout(model, WorkoutFocus.PULL, nowEpochMillis = 0L),
                bodyweightKg = 74.2,
                hasWorkoutInProgress = false,
            ),
            onStart = {},
            navigation = FocusWorkoutNavigation(onBack = {}, onOpenWorkout = {}, onOpenExercise = {}),
        )
    }
}
