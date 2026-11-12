package app.levitx.phone.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.history.ExerciseVisit
import app.levitx.core.load.formatKg
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.BackHeader
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.SetSquare
import app.levitx.phone.ui.StatStrip
import app.levitx.phone.ui.measureText
import app.levitx.phone.ui.shortDate

/** Exercise progress destination: connects [ExerciseProgressViewModel] to [ExerciseProgressScreen]. */
@Composable
fun ExerciseProgressRoute(
    exerciseId: String,
    navigation: ExerciseProgressNavigation,
    viewModel: ExerciseProgressViewModel = hiltViewModel<ExerciseProgressViewModel, ExerciseProgressViewModel.Factory>(
        creationCallback = { factory -> factory.create(exerciseId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseProgressScreen(uiState, navigation)
}

/**
 * One exercise's progress, as the Exercise progress artboard: the best set,
 * the best set of each recent workout against the next target, records, and
 * the last few workouts.
 */
@Composable
fun ExerciseProgressScreen(
    uiState: ExerciseProgressUiState,
    navigation: ExerciseProgressNavigation,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val ready = uiState as? ExerciseProgressUiState.Ready
        BackHeader(stringResource(R.string.progress_title), navigation.onBack) {
            ready?.let {
                Caption(
                    text = stringResource(R.string.exercise_progress_how_to),
                    color = LevitxColors.Accent,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = { navigation.onOpenHowTo(it.exerciseId) })
                        .padding(12.dp),
                )
            }
        }
        when (uiState) {
            ExerciseProgressUiState.Loading -> Unit

            ExerciseProgressUiState.Unavailable -> Lede(
                stringResource(R.string.data_unavailable),
                Modifier.padding(ScreenPadding),
            )

            ExerciseProgressUiState.NotFound -> Lede(
                stringResource(R.string.exercise_progress_none),
                Modifier.padding(ScreenPadding),
            )

            is ExerciseProgressUiState.Ready -> Column(Modifier.verticalScroll(rememberScrollState())) {
                Best(uiState)
                BestSets(uiState)
                StatStrip(
                    figures = listOf(
                        (uiState.kgPerRep?.let(::formatKg) ?: stringResource(R.string.no_value)) to
                            stringResource(R.string.figure_kg_per_rep),
                        (
                            uiState.nextTarget?.let { measureText(uiState.measure, it) }
                                ?: stringResource(R.string.no_value)
                            ) to
                            stringResource(R.string.exercise_progress_next_target),
                        uiState.workouts.toString() to stringResource(R.string.exercise_progress_workouts),
                    ),
                    modifier = Modifier.padding(horizontal = ScreenPadding),
                )
                Rule()
                RecordsSection(uiState)
                Recent(uiState, navigation.onOpenWorkout)
            }
        }
    }
}

/** The exercise and its best set, big. */
@Composable
private fun Best(state: ExerciseProgressUiState.Ready) {
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp, bottom = 22.dp)) {
        Caption(stringResource(R.string.exercise_progress_best, state.name))
        Text(
            text = measureText(state.measure, state.records.bestSet.value),
            style = LevitxType.Hero.copy(fontSize = 96.sp, lineHeight = 80.sp),
            color = LevitxColors.Text,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    Rule()
}

/** The best set of each recent workout as bars, with the next target dashed across. */
@Composable
private fun BestSets(state: ExerciseProgressUiState.Ready) {
    SectionTitle(
        stringResource(R.string.exercise_progress_chart),
        state.nextTarget?.let { stringResource(R.string.exercise_progress_target_key) },
    )
    val description = pluralStringResource(
        R.plurals.exercise_progress_chart_description,
        state.bars.size,
        state.bars.size,
        state.bars.joinToString { it.value.toString() },
    )
    val top = maxOf(state.bars.maxOf { it.value }, state.nextTarget ?: 0).coerceAtLeast(1)
    Canvas(
        modifier = Modifier
            .padding(horizontal = ScreenPadding)
            .fillMaxWidth()
            .height(120.dp)
            .semantics { contentDescription = description },
    ) {
        val gap = 4.dp.toPx()
        val width = (size.width - gap * (BARS_SHOWN - 1)) / BARS_SHOWN
        state.bars.forEachIndexed { index, bar ->
            val height = size.height * bar.value / top
            val color = if (index == state.bars.lastIndex) LevitxColors.Accent else LevitxColors.Text
            drawRect(color, Offset(index * (width + gap), size.height - height), Size(width, height))
        }
        state.nextTarget?.let { target ->
            val y = size.height * (1 - target.toFloat() / top)
            drawLine(
                LevitxColors.Ice,
                Offset(0f, y),
                Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
            )
        }
    }
    Row(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 8.dp, bottom = 22.dp)) {
        Caption(shortDate(state.bars.first().atEpochMillis), color = LevitxColors.TextFaint)
        Spacer(Modifier.weight(1f))
        Caption(shortDate(state.bars.last().atEpochMillis), color = LevitxColors.TextFaint)
    }
    Rule()
}

/** Best set, heaviest rep and most in one workout, each with its day. */
@Composable
private fun RecordsSection(state: ExerciseProgressUiState.Ready) {
    SectionTitle(stringResource(R.string.exercise_progress_records))
    val records = state.records
    Column(modifier = Modifier.padding(horizontal = ScreenPadding)) {
        RecordRow(
            stringResource(R.string.exercise_progress_record_set),
            records.bestSet.atEpochMillis,
            measureText(state.measure, records.bestSet.value),
        )
        records.heaviestKg?.let {
            RecordRow(
                stringResource(R.string.exercise_progress_record_heaviest),
                it.atEpochMillis,
                stringResource(R.string.kg_per_rep, formatKg(it.value)),
            )
        }
        RecordRow(
            stringResource(R.string.exercise_progress_record_most),
            records.mostInOneGo.atEpochMillis,
            measureText(state.measure, records.mostInOneGo.value),
        )
    }
}

@Composable
private fun RecordRow(name: String, atEpochMillis: Long, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LevitxColors.Text)
            Caption(shortDate(atEpochMillis), color = LevitxColors.TextFaint)
        }
        Text(value, style = LevitxType.Figure, color = LevitxColors.Text)
    }
    Rule()
}

/** The last few workouts with every set, each opening the workout. */
@Composable
private fun Recent(state: ExerciseProgressUiState.Ready, onOpenWorkout: (String) -> Unit) {
    SectionTitle(stringResource(R.string.exercise_progress_recent))
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, bottom = 24.dp)) {
        state.recent.forEach { visit -> RecentRow(visit, onOpenWorkout) }
    }
}

@Composable
private fun RecentRow(visit: ExerciseVisit, onOpenWorkout: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = { onOpenWorkout(visit.workout.id) })
            .padding(vertical = 10.dp),
    ) {
        Caption(shortDate(visit.workout.finishedAtEpochMillis), modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            visit.record.sets.forEach { SetSquare(visit.record.exercise?.measure ?: GateMeasure.REPS, it) }
        }
    }
    Rule()
}
