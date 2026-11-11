package app.levitx.phone.train

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.WorkoutFocus
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.BackButton
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Help
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.PhotoHeader
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.SessionList
import app.levitx.phone.ui.focusPhoto
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

/** How far the title rides up over the photo's faded edge. */
private val TitleOverlap = (-20).dp
private val WorkoutPhotoHeight = 150.dp
private val WorkoutBarHeight = 64.dp

/**
 * A single workout, laid out as the "Workout detail" artboard: its photo, its
 * name and size, the moves with their load per rep, and Start pinned to the
 * bottom.
 */
@Composable
fun FocusWorkoutScreen(
    uiState: FocusWorkoutUiState,
    onStart: () -> Unit,
    navigation: FocusWorkoutNavigation,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(LevitxColors.Background).navigationBarsPadding()) {
        when (uiState) {
            FocusWorkoutUiState.Loading -> Unit

            FocusWorkoutUiState.Unavailable -> Message(stringResource(R.string.data_unavailable), navigation.onBack)

            FocusWorkoutUiState.NotFound -> Message(stringResource(R.string.focus_workout_not_found), navigation.onBack)

            is FocusWorkoutUiState.Ready -> {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    PhotoHeader(focusPhoto(uiState.focus), navigation.onBack, height = WorkoutPhotoHeight)
                    Workout(uiState, navigation.onOpenExercise)
                }
                if (uiState.session != null) StartBar(uiState.hasWorkoutInProgress, onStart)
            }
        }
    }
}

/** A sentence, with back. */
@Composable
private fun Message(text: String, onBack: () -> Unit) {
    BackButton(onBack, modifier = Modifier.statusBarsPadding().padding(4.dp))
    Lede(text, modifier = Modifier.padding(ScreenPadding))
}

/** The name, minutes and moves, then the moves themselves. */
@Composable
private fun Workout(state: FocusWorkoutUiState.Ready, onOpenExercise: (String) -> Unit) {
    val session = state.session
    Column(modifier = Modifier.offset(y = TitleOverlap).padding(horizontal = ScreenPadding)) {
        Text(
            text = focusName(state.focus),
            style = LevitxType.Poster.copy(fontSize = 40.sp, lineHeight = 38.sp),
            color = LevitxColors.Text,
            modifier = Modifier.semantics { heading() },
        )
        Caption(
            text = if (session == null) {
                stringResource(R.string.train_nothing_safe)
            } else {
                pluralStringResource(
                    R.plurals.train_workout_meta,
                    session.plans.size,
                    session.estimatedMinutes,
                    session.plans.size,
                )
            },
            modifier = Modifier.padding(top = 10.dp),
        )
    }
    if (session == null) {
        Lede(stringResource(R.string.focus_workout_nothing), modifier = Modifier.padding(horizontal = ScreenPadding))
        return
    }
    SessionList(session, state.bodyweightKg, onExerciseClick = onOpenExercise)
    Help(
        text = stringResource(R.string.focus_workout_note),
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 14.dp, bottom = 14.dp),
    )
}

/** Start workout, or Resume when one is already going (with why). */
@Composable
private fun StartBar(inProgress: Boolean, onStart: () -> Unit) {
    if (inProgress) {
        Help(
            text = stringResource(R.string.focus_workout_in_progress),
            modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, bottom = 10.dp),
        )
    }
    AccentBar(
        text = stringResource(if (inProgress) R.string.focus_workout_resume else R.string.focus_workout_start),
        onClick = onStart,
        height = WorkoutBarHeight,
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
