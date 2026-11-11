package app.levitx.phone.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
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
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Chip
import app.levitx.phone.ui.ChipRow
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.StatStrip
import app.levitx.phone.ui.TabHeader
import app.levitx.phone.ui.measureText
import app.levitx.phone.ui.patternName

private val SkillsBarHeight = 64.dp
private val RailHeight = 28.dp
private val RailLine = 2.dp
private val MarkSize = 10.dp
private val CurrentMarkSize = 16.dp
private val MarkBorder = 2.dp

/**
 * Skills tab: connects [SkillsViewModel] to [SkillsScreen].
 *
 * @param onOpenExercise Opens an exercise's detail, by id.
 */
@Composable
fun SkillsRoute(onOpenExercise: (String) -> Unit, viewModel: SkillsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SkillsScreen(uiState, onOpenExercise)
}

/**
 * The Skills tab, laid out as the "Skill ladder" artboard: a chip for each
 * movement, then the chosen movement's ladder with the exercise trained now
 * as the title, its rungs as squares on a line, the figures that matter, and
 * what opens next. How to do it is pinned to the bottom.
 */
@Composable
fun SkillsScreen(uiState: SkillsUiState, onOpenExercise: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(LevitxColors.Background)) {
        TabHeader(stringResource(R.string.tab_skills))
        when (uiState) {
            SkillsUiState.Loading -> Unit

            SkillsUiState.Unavailable -> Lede(
                stringResource(R.string.data_unavailable),
                Modifier.padding(ScreenPadding),
            )

            is SkillsUiState.Ready -> Ladders(uiState.ladders, onOpenExercise)
        }
    }
}

/** The chips, the chosen ladder, and how to do its exercise. */
@Composable
private fun ColumnScope.Ladders(ladders: List<LadderStanding>, onOpenExercise: (String) -> Unit) {
    // Starts on the movement the goal trains, the one that matters most.
    var pattern by rememberSaveable {
        mutableStateOf((ladders.firstOrNull { it.isTrainedByGate } ?: ladders.first()).pattern)
    }
    val standing = ladders.firstOrNull { it.pattern == pattern } ?: ladders.first()
    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        ChipRow(modifier = Modifier.padding(top = 16.dp)) {
            ladders.forEach { ladder ->
                Chip(patternName(ladder.pattern), selected = ladder.pattern == pattern, onClick = {
                    pattern = ladder.pattern
                })
            }
        }
        Ladder(standing)
        Figures(standing)
        NextCard(standing)
    }
    val exercise = standing.exercise
    if (exercise != null && !standing.isTrainedByGate) {
        AccentBar(
            text = stringResource(R.string.skills_how_to, exercise.name),
            onClick = { onOpenExercise(exercise.id) },
            height = SkillsBarHeight,
        )
    }
}

/** "PULLING UP · STEP 2 OF 6", the exercise as the title, and the rungs on a line. */
@Composable
private fun Ladder(standing: LadderStanding) {
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 24.dp)) {
        Caption(
            stringResource(R.string.skills_step, patternName(standing.pattern), standing.rung + 1, standing.rungCount),
        )
        Text(
            text = standing.exercise?.name ?: stringResource(R.string.skills_no_kit_title),
            fontSize = 36.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            color = LevitxColors.Text,
            modifier = Modifier.padding(top = 12.dp).semantics { heading() },
        )
        Rungs(standing)
    }
}

/**
 * The ladder's rungs as squares on a line: done filled, the one trained now
 * larger and orange, the next one an ice outline, the rest dark outlines.
 */
@Composable
private fun Rungs(standing: LadderStanding) {
    val description = stringResource(
        R.string.skills_rungs_description,
        standing.rung + 1,
        standing.rungCount,
    )
    val doneFraction = standing.rung.toFloat() / (standing.rungCount - 1).coerceAtLeast(1)
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 24.dp)
            .fillMaxWidth()
            .height(RailHeight)
            .drawBehind {
                val y = size.height / 2
                drawLine(LevitxColors.Track, Offset(0f, y), Offset(size.width, y), RailLine.toPx())
                drawLine(LevitxColors.Text, Offset(0f, y), Offset(size.width * doneFraction, y), RailLine.toPx())
            }
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        repeat(standing.rungCount) { index ->
            when {
                index < standing.rung -> Box(Modifier.size(MarkSize).background(LevitxColors.Text))

                index == standing.rung -> Box(Modifier.size(CurrentMarkSize).background(LevitxColors.Accent))

                else -> Box(
                    Modifier
                        .size(MarkSize)
                        .background(LevitxColors.Background)
                        .border(MarkBorder, if (index == standing.nextRung) LevitxColors.Ice else LevitxColors.Tick),
                )
            }
        }
    }
}

/** The target now, the step, and the rungs left to the top. */
@Composable
private fun Figures(standing: LadderStanding) {
    val exercise = standing.exercise
    val none = stringResource(R.string.no_value)
    Rule(Modifier.padding(top = 20.dp))
    StatStrip(
        figures = listOf(
            (
                if (exercise != null &&
                    standing.target != null
                ) {
                    measureText(exercise.measure, standing.target)
                } else {
                    none
                }
                ) to
                stringResource(R.string.skills_target_label),
            stringResource(R.string.skills_step_figure, standing.rung + 1, standing.rungCount) to
                stringResource(R.string.skills_step_label),
            (standing.rungCount - standing.rung - 1).toString() to stringResource(R.string.skills_left_label),
        ),
        modifier = Modifier.padding(horizontal = ScreenPadding),
    )
    Rule()
}

/** The ice-edged card: what opens next and how, or why this ladder is quiet for now. */
@Composable
private fun NextCard(standing: LadderStanding) {
    val exercise = standing.exercise
    val (label, title, body) = when {
        standing.isTrainedByGate -> Triple(
            stringResource(R.string.skills_gate_label),
            stringResource(R.string.skills_gate_title),
            stringResource(R.string.skills_gate),
        )

        exercise == null -> Triple(
            stringResource(R.string.skills_kit_label),
            stringResource(R.string.skills_no_kit_title),
            stringResource(R.string.skills_no_kit),
        )

        standing.next == null -> Triple(
            stringResource(R.string.skills_top_label),
            stringResource(R.string.skills_top),
            stringResource(R.string.skills_top_body),
        )

        else -> Triple(
            stringResource(R.string.skills_next_label, standing.next.name),
            stringResource(R.string.skills_next_title),
            stringResource(R.string.skills_next_body),
        )
    }
    Column(
        modifier = Modifier
            .padding(start = ScreenPadding, end = ScreenPadding, top = 16.dp, bottom = 24.dp)
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .border(1.dp, LevitxColors.Ice)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
    ) {
        Caption(label, color = LevitxColors.Ice)
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = LevitxColors.Text,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = body,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = LevitxColors.TextMuted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Preview
@Composable
private fun SkillsScreenPreview() {
    val profile = CoachProfile("first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR))
    LevitxTheme {
        SkillsScreen(
            SkillsUiState.Ready(standings(AthleteModel.starting(profile, PullUpAnswer.NOT_YET))),
            onOpenExercise = {},
        )
    }
}
