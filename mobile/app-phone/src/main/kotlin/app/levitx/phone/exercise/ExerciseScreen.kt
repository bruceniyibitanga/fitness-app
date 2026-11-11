package app.levitx.phone.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.BackButton
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Help
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.PhotoHeader
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.StatStrip
import app.levitx.phone.ui.measureText
import app.levitx.phone.ui.patternName
import app.levitx.phone.ui.patternPhoto
import kotlin.math.roundToInt

private val NoteMarkSize = 6.dp
private val RungMarkSize = 12.dp
private val RungRowHeight = 42.dp
private val ExercisePhotoHeight = 180.dp
private val SelectedEdge = 2.dp
private const val PERCENT = 100

/** Figures in the strip. Three fit side by side at any font size. */
private const val MAX_FIGURES = 3

/** How far the title rides up over the photo's faded edge. */
private val TitleOverlap = (-18).dp

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

/**
 * One exercise, laid out as the "Exercise detail" artboard: its photo and
 * name, the person's numbers on it, how to do it, and where it sits on its
 * ladder.
 */
@Composable
fun ExerciseScreen(uiState: ExerciseUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        when (uiState) {
            ExerciseUiState.Loading -> Unit

            ExerciseUiState.Unavailable -> Message(stringResource(R.string.data_unavailable), onBack)

            ExerciseUiState.NotFound -> Message(stringResource(R.string.exercise_not_found), onBack)

            is ExerciseUiState.Ready -> {
                PhotoHeader(patternPhoto(uiState.exercise.pattern), onBack, height = ExercisePhotoHeight)
                Exercise(uiState)
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

@Composable
private fun Exercise(state: ExerciseUiState.Ready) {
    val exercise = state.exercise
    Column(modifier = Modifier.offset(y = TitleOverlap).padding(horizontal = ScreenPadding)) {
        Text(
            text = exercise.name,
            style = LevitxType.Poster.copy(fontSize = 40.sp, lineHeight = 38.sp),
            color = LevitxColors.Text,
            modifier = Modifier.semantics { heading() },
        )
        Caption(patternName(exercise.pattern), modifier = Modifier.padding(top = 8.dp))
    }
    Numbers(state)
    state.guide?.let { guide ->
        Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 14.dp, bottom = 14.dp)) {
            Caption(stringResource(R.string.exercise_cues), modifier = Modifier.padding(bottom = 8.dp))
            guide.cues.forEachIndexed { index, cue ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text(
                        text = "${index + 1}.",
                        fontSize = 14.sp,
                        color = LevitxColors.TextMuted,
                        modifier = Modifier.width(18.dp),
                    )
                    Text(cue, fontSize = 14.sp, lineHeight = 19.sp, color = LevitxColors.Text)
                }
            }
        }
        Rule()
        guide.note?.let { Note(it) }
    }
    val ladder = state.ladder
    if (ladder == null) Note(stringResource(R.string.exercise_skill)) else Ladder(ladder)
}

/** Load per rep, the share of bodyweight it moves, and the target and best. */
@Composable
private fun Numbers(state: ExerciseUiState.Ready) {
    val exercise = state.exercise
    val profile = exercise.loadProfile
    val bodyweight = state.bodyweightKg
    val figures = buildList {
        if (profile != null && bodyweight != null) {
            add(
                formatKg(calculateEffectiveLoad(bodyweight, profile).totalKg) to
                    stringResource(R.string.exercise_kg_per_rep),
            )
        }
        if (profile != null) {
            val percent = (profile.bodyweightShare * PERCENT).roundToInt()
            add(
                stringResource(R.string.percent, percent) to stringResource(
                    if (profile.evidence == LoadEvidence.MEASURED) {
                        R.string.exercise_share_measured
                    } else {
                        R.string.exercise_share_estimate
                    },
                ),
            )
        }
        state.target?.let { add(measureText(exercise.measure, it) to stringResource(R.string.exercise_target)) }
        state.best?.let { add(measureText(exercise.measure, it) to stringResource(R.string.exercise_best)) }
    }.take(MAX_FIGURES)
    if (figures.isEmpty()) return
    Rule()
    StatStrip(figures, modifier = Modifier.padding(horizontal = ScreenPadding))
    Rule()
}

/** A small ice square and a line of advice. */
@Composable
private fun Note(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 12.dp),
    ) {
        Box(
            Modifier
                .padding(top = 5.dp)
                .size(NoteMarkSize)
                .background(LevitxColors.Ice),
        )
        Help(text)
    }
}

/** Every rung of the ladder, easiest first, with the person's place on it. */
@Composable
private fun Ladder(rungs: List<LadderRung>) {
    Rule()
    Caption(
        stringResource(R.string.exercise_ladder),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(horizontal = ScreenPadding, vertical = 12.dp),
    )
    Rule()
    rungs.forEach { rung ->
        val current = rung.status == RungStatus.CURRENT
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (rung.holdsShown) LevitxColors.Surface else LevitxColors.Background)
                .drawBehind {
                    if (rung.holdsShown) drawRect(LevitxColors.Accent, size = size.copy(width = SelectedEdge.toPx()))
                }
                .heightIn(min = RungRowHeight)
                .padding(horizontal = ScreenPadding, vertical = 6.dp),
        ) {
            when (rung.status) {
                RungStatus.DONE -> Box(Modifier.size(RungMarkSize).background(LevitxColors.TextFaint))
                RungStatus.CURRENT -> Box(Modifier.size(RungMarkSize).background(LevitxColors.Accent))
                RungStatus.LATER -> Box(Modifier.size(RungMarkSize).border(2.dp, LevitxColors.Outline))
            }
            Text(
                text = rung.names.reduce { first, second -> stringResource(R.string.exercise_or, first, second) },
                fontSize = 14.sp,
                fontWeight = if (rung.holdsShown) FontWeight.SemiBold else FontWeight.Normal,
                color = if (rung.status == RungStatus.DONE) LevitxColors.TextFaint else LevitxColors.Text,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(
                    when (rung.status) {
                        RungStatus.DONE -> R.string.exercise_rung_done
                        RungStatus.CURRENT -> R.string.exercise_rung_current
                        RungStatus.LATER -> R.string.exercise_rung_later
                    },
                ),
                style = LevitxType.Micro,
                color = if (current) LevitxColors.Accent else LevitxColors.TextFaint,
            )
        }
        Rule()
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
