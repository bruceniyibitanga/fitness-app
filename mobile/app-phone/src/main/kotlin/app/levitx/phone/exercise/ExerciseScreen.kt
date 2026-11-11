package app.levitx.phone.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.load.LoadEvidence
import app.levitx.core.load.calculateEffectiveLoad
import app.levitx.core.load.formatKg
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.SectionLabel
import app.levitx.phone.ui.measureText
import app.levitx.phone.ui.patternName
import kotlin.math.roundToInt

private val NoteMarkSize = 6.dp
private val RungRowHeight = 44.dp
private const val PERCENT = 100

/**
 * An exercise's destination: connects [ExerciseViewModel] to [ExerciseScreen].
 *
 * @param exerciseId The exercise's id from the back stack.
 * @param onBack Leaves the screen.
 */
@Composable
fun ExerciseRoute(
    exerciseId: String,
    onBack: () -> Unit,
    viewModel: ExerciseViewModel = hiltViewModel<ExerciseViewModel, ExerciseViewModel.Factory>(
        creationCallback = { factory -> factory.create(exerciseId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseScreen(uiState, onBack)
}

/** How to do one exercise, the person's numbers on it, and where it sits on its ladder. */
@Composable
fun ExerciseScreen(uiState: ExerciseUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Medium),
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = LevitxSpacing.Medium, end = LevitxSpacing.Medium, bottom = LevitxSpacing.Large),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            when (uiState) {
                ExerciseUiState.Loading -> Unit
                ExerciseUiState.Unavailable -> Text(stringResource(R.string.data_unavailable))
                ExerciseUiState.NotFound -> Text(stringResource(R.string.exercise_not_found))
                is ExerciseUiState.Ready -> Exercise(uiState)
            }
        }
    }
}

@Composable
private fun Exercise(state: ExerciseUiState.Ready) {
    val exercise = state.exercise
    SectionLabel(patternName(exercise.pattern), color = LevitxColors.Accent)
    Text(exercise.name, style = LevitxType.Display, modifier = Modifier.semantics { heading() })
    Numbers(state)
    state.guide?.let { guide ->
        SectionLabel(stringResource(R.string.exercise_cues))
        guide.cues.forEachIndexed { index, cue ->
            Row(horizontalArrangement = Arrangement.spacedBy(LevitxSpacing.Small)) {
                Text("${index + 1}.", style = MaterialTheme.typography.bodyLarge, color = LevitxColors.TextMuted)
                Text(cue, style = MaterialTheme.typography.bodyLarge)
            }
        }
        guide.note?.let { Note(it) }
    }
    val ladder = state.ladder
    if (ladder == null) {
        Note(stringResource(R.string.exercise_skill))
    } else {
        Ladder(ladder)
    }
}

/** The person's target and best, and the load per rep where the exercise has one. */
@Composable
private fun Numbers(state: ExerciseUiState.Ready) {
    val exercise = state.exercise
    val profile = exercise.loadProfile
    HorizontalDivider(color = LevitxColors.Line)
    Row(horizontalArrangement = Arrangement.spacedBy(LevitxSpacing.Large), modifier = Modifier.fillMaxWidth()) {
        state.target?.let { Figure(measureText(exercise.measure, it), stringResource(R.string.exercise_target)) }
        state.best?.let { Figure(measureText(exercise.measure, it), stringResource(R.string.exercise_best)) }
        val bodyweight = state.bodyweightKg
        if (profile != null && bodyweight != null) {
            Figure(
                formatKg(calculateEffectiveLoad(bodyweight, profile).totalKg),
                stringResource(R.string.exercise_kg_per_rep),
            )
        }
    }
    if (profile != null) {
        val percent = (profile.bodyweightShare * PERCENT).roundToInt().toString()
        SectionLabel(
            stringResource(
                if (profile.evidence == LoadEvidence.MEASURED) {
                    R.string.exercise_share_measured
                } else {
                    R.string.exercise_share_estimate
                },
                percent,
            ),
        )
    }
    HorizontalDivider(color = LevitxColors.Line)
}

@Composable
private fun Figure(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(LevitxSpacing.XSmall)) {
        Text(value, style = LevitxType.Numeral)
        SectionLabel(label)
    }
}

@Composable
private fun Note(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(LevitxSpacing.Small)) {
        Box(
            Modifier
                .padding(top = LevitxSpacing.Small)
                .size(NoteMarkSize)
                .background(LevitxColors.Ice),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = LevitxColors.TextSoft)
    }
}

/** Every rung of the ladder, easiest first, with the person's place on it. */
@Composable
private fun Ladder(rungs: List<LadderRung>) {
    Column {
        SectionLabel(
            stringResource(R.string.exercise_ladder),
            modifier = Modifier.padding(bottom = LevitxSpacing.Small),
        )
        HorizontalDivider(color = LevitxColors.Line)
        rungs.forEach { rung ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (rung.holdsShown) LevitxColors.Surface else LevitxColors.Background)
                    .heightIn(min = RungRowHeight)
                    .padding(horizontal = LevitxSpacing.Small, vertical = LevitxSpacing.XSmall),
            ) {
                Text(
                    text = rung.names.reduce { first, second -> stringResource(R.string.exercise_or, first, second) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (rung.holdsShown) FontWeight.Bold else FontWeight.Normal,
                    color = if (rung.status == RungStatus.DONE) LevitxColors.TextMuted else LevitxColors.Text,
                    modifier = Modifier.weight(1f),
                )
                SectionLabel(
                    text = stringResource(
                        when (rung.status) {
                            RungStatus.DONE -> R.string.exercise_rung_done
                            RungStatus.CURRENT -> R.string.exercise_rung_current
                            RungStatus.LATER -> R.string.exercise_rung_later
                        },
                    ),
                    color = if (rung.status == RungStatus.CURRENT) LevitxColors.Accent else LevitxColors.TextMuted,
                )
            }
            HorizontalDivider(color = LevitxColors.Line)
        }
    }
}

@Preview
@Composable
private fun ExerciseScreenPreview() {
    val model = AthleteModel.starting(
        CoachProfile("first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR)),
        PullUpAnswer.YES,
    )
    LevitxTheme {
        ExerciseScreen(uiState = exerciseState(model, "horizontal_push.push_up", nowEpochMillis = 0L), onBack = {})
    }
}
