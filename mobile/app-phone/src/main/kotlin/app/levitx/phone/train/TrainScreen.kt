package app.levitx.phone.train

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.WorkoutFocus
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Chip
import app.levitx.phone.ui.ChipRow
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.TabHeader

/**
 * Train tab: connects [TrainViewModel] to [TrainScreen].
 *
 * @param onOpenWorkout Opens the workout for a focus, by its name.
 * @param onOpenProgram Opens a skill program, by its id.
 */
@Composable
fun TrainRoute(
    onOpenWorkout: (String) -> Unit,
    onOpenProgram: (String) -> Unit,
    viewModel: TrainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TrainScreen(uiState, onOpenWorkout, onOpenProgram)
}

/**
 * The Train tab, laid out as the Train artboard: a row of body-focus chips,
 * skill programs as photo cards, and a single workout for each part of the
 * body as photo rows. A chip narrows the workouts to that focus.
 */
@Composable
fun TrainScreen(
    uiState: TrainUiState,
    onOpenWorkout: (String) -> Unit,
    onOpenProgram: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Survives rotation and coming back from a workout. Null is "All".
    var focus by rememberSaveable { mutableStateOf<WorkoutFocus?>(null) }
    Column(modifier = modifier.fillMaxSize().background(LevitxColors.Background)) {
        TabHeader(stringResource(R.string.train_label))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            when (uiState) {
                TrainUiState.Loading -> Unit

                TrainUiState.Unavailable -> Lede(
                    text = stringResource(R.string.data_unavailable),
                    modifier = Modifier.padding(ScreenPadding),
                )

                is TrainUiState.Ready -> {
                    FocusChips(selected = focus, onSelect = { focus = it })
                    Programs(uiState.programs, onOpenProgram)
                    Workouts(uiState.workouts.filter { focus == null || it.focus == focus }, onOpenWorkout)
                }
            }
        }
    }
}

/** "BODY FOCUS" and a chip for All and each focus, scrolling sideways. */
@Composable
private fun FocusChips(selected: WorkoutFocus?, onSelect: (WorkoutFocus?) -> Unit) {
    Caption(
        stringResource(R.string.train_body_focus),
        modifier = Modifier.padding(start = ScreenPadding, top = 16.dp, bottom = 10.dp),
    )
    ChipRow {
        Chip(stringResource(R.string.train_all), selected = selected == null, onClick = { onSelect(null) })
        WorkoutFocus.entries.forEach { focus ->
            Chip(focusName(focus), selected = selected == focus, onClick = { onSelect(focus) })
        }
    }
}

/** The programs heading and the cards, scrolling sideways. */
@Composable
private fun Programs(programs: List<ProgramSummary>, onOpen: (String) -> Unit) {
    Caption(
        stringResource(R.string.train_programs),
        modifier = Modifier.padding(start = ScreenPadding, top = 20.dp, bottom = 10.dp),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding),
    ) {
        programs.forEach { program -> ProgramCard(program, onClick = { onOpen(program.path.id) }) }
    }
}

/** The single workouts heading and a row for each. */
@Composable
private fun Workouts(workouts: List<WorkoutSummary>, onOpen: (String) -> Unit) {
    Caption(
        stringResource(R.string.train_workouts),
        modifier = Modifier.padding(start = ScreenPadding, top = 20.dp, bottom = 4.dp),
    )
    workouts.forEach { workout ->
        WorkoutRow(workout, onClick = { onOpen(workout.focus.name) })
        Rule()
    }
}

@Preview
@Composable
private fun TrainScreenPreview() {
    val model = AthleteModel.starting(
        CoachProfile("first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR, Equipment.LOW_BAR)),
        PullUpAnswer.NOT_YET,
    )
    LevitxTheme {
        TrainScreen(
            uiState = TrainUiState.Ready(programsFor(model), workoutsFor(model, Coach(), nowEpochMillis = 0L)),
            onOpenWorkout = {},
            onOpenProgram = {},
        )
    }
}
