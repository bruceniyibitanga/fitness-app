package app.levitx.phone.train

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.SectionLabel

private val ProgramCardWidth = 220.dp
private val WorkoutRowHeight = 68.dp

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

/** Skill programs to make the plan, and a workout for each part of the body to do now. */
@Composable
fun TrainScreen(
    uiState: TrainUiState,
    onOpenWorkout: (String) -> Unit,
    onOpenProgram: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = LevitxSpacing.Large),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Small),
            modifier = Modifier.padding(horizontal = LevitxSpacing.Medium),
        ) {
            SectionLabel(stringResource(R.string.train_label), color = LevitxColors.Accent)
            Text(stringResource(R.string.train_title), style = LevitxType.Display)
            Text(
                text = stringResource(R.string.train_body),
                style = MaterialTheme.typography.bodyLarge,
                color = LevitxColors.TextSoft,
            )
        }
        when (uiState) {
            TrainUiState.Loading -> Unit

            TrainUiState.Unavailable -> Text(
                text = stringResource(R.string.data_unavailable),
                modifier = Modifier.padding(LevitxSpacing.Medium),
            )

            is TrainUiState.Ready -> {
                Programs(uiState.programs, onOpenProgram)
                Workouts(uiState.workouts, onOpenWorkout)
            }
        }
    }
}

@Composable
private fun Programs(programs: List<ProgramSummary>, onOpen: (String) -> Unit) {
    SectionLabel(
        text = stringResource(R.string.train_programs),
        modifier = Modifier.padding(
            start = LevitxSpacing.Medium,
            top = LevitxSpacing.Large,
            bottom = LevitxSpacing.Small,
        ),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(LevitxSpacing.Small),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = LevitxSpacing.Medium),
    ) {
        programs.forEach { program ->
            Column(
                verticalArrangement = Arrangement.spacedBy(LevitxSpacing.XSmall),
                modifier = Modifier
                    .width(ProgramCardWidth)
                    .background(LevitxColors.Surface)
                    .clickable(role = Role.Button) { onOpen(program.path.id) }
                    .padding(LevitxSpacing.Medium),
            ) {
                if (program.isCurrent) {
                    SectionLabel(stringResource(R.string.train_current_program), color = LevitxColors.Accent)
                }
                Text(program.path.name, style = MaterialTheme.typography.titleMedium)
                SectionLabel(
                    pluralStringResource(
                        R.plurals.train_program_gates,
                        program.path.gates.size,
                        program.path.gates.size,
                        program.passed,
                    ),
                )
            }
        }
    }
}

@Composable
private fun Workouts(workouts: List<WorkoutSummary>, onOpen: (String) -> Unit) {
    SectionLabel(
        text = stringResource(R.string.train_workouts),
        modifier = Modifier.padding(
            start = LevitxSpacing.Medium,
            top = LevitxSpacing.Large,
            bottom = LevitxSpacing.Small,
        ),
    )
    HorizontalDivider(color = LevitxColors.Line)
    workouts.forEach { workout ->
        val session = workout.session
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = WorkoutRowHeight)
                .clickable(role = Role.Button) { onOpen(workout.focus.name) }
                .padding(horizontal = LevitxSpacing.Medium, vertical = LevitxSpacing.Small),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(LevitxSpacing.XSmall), modifier = Modifier.weight(1f)) {
                Text(focusName(workout.focus), style = MaterialTheme.typography.titleMedium)
                if (session == null) {
                    Text(
                        text = stringResource(R.string.train_nothing_safe),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LevitxColors.TextMuted,
                    )
                } else {
                    SectionLabel(
                        pluralStringResource(
                            R.plurals.train_workout_meta,
                            session.plans.size,
                            session.estimatedMinutes,
                            session.plans.size,
                        ),
                    )
                }
            }
            if (session != null && session.plans.any { !it.isHard }) {
                SectionLabel(stringResource(R.string.train_some_light), color = LevitxColors.Ice)
            }
        }
        HorizontalDivider(color = LevitxColors.Line)
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
