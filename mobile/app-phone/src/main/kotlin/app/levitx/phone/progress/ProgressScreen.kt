package app.levitx.phone.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.history.LoadPoint
import app.levitx.core.load.formatKg
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.BackHeader
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.patternName
import app.levitx.phone.ui.shortDate
import app.levitx.phone.ui.signedKg

/** Size of the marks on the load chart. */
private val MarkSize = 8.dp

/**
 * Where the progress screen can go.
 *
 * @property onBack Back to where it was opened from.
 * @property onOpenExercise One exercise's progress, by id.
 * @property onOpenHistory Every workout, by month.
 */
data class ProgressNavigation(
    val onBack: () -> Unit,
    val onOpenExercise: (String) -> Unit,
    val onOpenHistory: () -> Unit,
)

/** Progress destination: connects [ProgressViewModel] to [ProgressScreen]. */
@Composable
fun ProgressRoute(navigation: ProgressNavigation, viewModel: ProgressViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(uiState, navigation, onChooseRange = viewModel::chooseRange)
}

/**
 * Progress, as the Progress artboard: load per rep over time for the lead
 * movement, every movement, this week's hard sets, workouts per week, and
 * gates passed.
 */
@Composable
fun ProgressScreen(
    uiState: ProgressUiState,
    navigation: ProgressNavigation,
    onChooseRange: (ProgressRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        BackHeader(stringResource(R.string.progress_title), navigation.onBack) {
            if (uiState is ProgressUiState.Ready) RangeToggle(uiState.range, onChooseRange)
        }
        when (uiState) {
            ProgressUiState.Loading -> Unit

            ProgressUiState.Unavailable -> Lede(
                stringResource(R.string.data_unavailable),
                Modifier.padding(ScreenPadding),
            )

            is ProgressUiState.Ready -> Column(Modifier.verticalScroll(rememberScrollState())) {
                if (uiState.workoutCount == 0) {
                    Lede(stringResource(R.string.progress_empty), Modifier.padding(ScreenPadding))
                } else {
                    HeadlineSection(uiState.headline)
                    Moves(uiState.moves, navigation.onOpenExercise)
                    WeekSetsSection(uiState.weekSets)
                    WeeksSection(uiState.weeks, navigation.onOpenHistory)
                    GatesSection(uiState.gates)
                }
            }
        }
    }
}

/** 4W, 3M, ALL: a small segmented choice. */
@Composable
private fun RangeToggle(selected: ProgressRange, onChoose: (ProgressRange) -> Unit) {
    Row(modifier = Modifier.selectableGroup()) {
        ProgressRange.entries.forEach { range ->
            val isSelected = range == selected
            Text(
                text = stringResource(
                    when (range) {
                        ProgressRange.FOUR_WEEKS -> R.string.progress_range_4w
                        ProgressRange.THREE_MONTHS -> R.string.progress_range_3m
                        ProgressRange.ALL -> R.string.progress_range_all
                    },
                ),
                style = LevitxType.Caption,
                color = if (isSelected) LevitxColors.Background else LevitxColors.TextMuted,
                modifier = Modifier
                    .background(if (isSelected) LevitxColors.Text else LevitxColors.Background)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onChoose(range) })
                    .padding(horizontal = 10.dp, vertical = 12.dp),
            )
        }
    }
}

/** The lead movement: kilograms per rep now, the change, and the step chart. */
@Composable
private fun HeadlineSection(headline: Headline?) {
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp, bottom = 22.dp)) {
        if (headline == null) {
            Caption(stringResource(R.string.progress_load_label_none))
            Lede(stringResource(R.string.progress_no_load), Modifier.padding(top = 8.dp))
            return@Column
        }
        Caption(stringResource(R.string.progress_load_label, patternName(headline.pattern)))
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 12.dp)) {
            Text(
                text = formatKg(headline.nowKg),
                style = LevitxType.Hero.copy(fontSize = 96.sp, lineHeight = 80.sp),
                color = LevitxColors.Text,
            )
            Caption(stringResource(R.string.kg), modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 6.dp)) {
                Text(signedKg(headline.changeKg), style = LevitxType.Figure, color = LevitxColors.Accent)
                Caption(stringResource(R.string.progress_since, shortDate(headline.points.first().atEpochMillis)))
            }
        }
        LoadChart(headline.points, Modifier.padding(top = 22.dp))
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Caption(shortDate(headline.points.first().atEpochMillis), color = LevitxColors.TextFaint)
            Spacer(Modifier.weight(1f))
            Caption(stringResource(R.string.progress_squares), color = LevitxColors.TextFaint)
            Spacer(Modifier.weight(1f))
            Caption(stringResource(R.string.progress_today), color = LevitxColors.TextFaint)
        }
        Text(
            text = stringResource(R.string.progress_load_note),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = LevitxColors.TextMuted,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    Rule()
}

/**
 * Kilograms per rep as steps, a workout at a time: a square where a harder
 * rung started, orange at the latest.
 */
@Composable
private fun LoadChart(points: List<LoadPoint>, modifier: Modifier = Modifier) {
    val description = pluralStringResource(
        R.plurals.progress_chart_description,
        points.size,
        formatKg(points.first().kgPerRep),
        formatKg(points.last().kgPerRep),
        points.size,
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .semantics { contentDescription = description },
    ) {
        val low = points.minOf { it.kgPerRep }
        val high = points.maxOf { it.kgPerRep }
        val mark = MarkSize.toPx()
        val span = (high - low).takeIf { it > 0.0 } ?: 1.0
        fun x(index: Int) = if (points.size == 1) size.width - mark else index * (size.width - mark) / (points.size - 1)
        fun y(kg: Double) = (size.height - mark) * (1 - ((kg - low) / span)).toFloat() + mark / 2
        val base = size.height - 1.dp.toPx() / 2
        drawLine(LevitxColors.Line, Offset(0f, base), Offset(size.width, base))
        points.zipWithNext().forEachIndexed { index, (a, b) ->
            // Flat until the next workout, then up or down: steps, not slopes.
            drawLine(
                LevitxColors.Text,
                Offset(x(index), y(a.kgPerRep)),
                Offset(x(index + 1), y(a.kgPerRep)),
                2.dp.toPx(),
            )
            drawLine(
                LevitxColors.Text,
                Offset(x(index + 1), y(a.kgPerRep)),
                Offset(x(index + 1), y(b.kgPerRep)),
                2.dp.toPx(),
            )
        }
        points.forEachIndexed { index, point ->
            if (point.isNewRung) {
                val topLeft = Offset(x(index) - mark / 2, y(point.kgPerRep) - mark / 2)
                drawRect(LevitxColors.Background, topLeft, Size(mark, mark))
                drawRect(LevitxColors.Text, topLeft, Size(mark, mark), style = Stroke(2.dp.toPx()))
            }
        }
        drawRect(
            LevitxColors.Accent,
            Offset(x(points.lastIndex) - mark / 2, y(points.last().kgPerRep) - mark / 2),
            Size(mark, mark),
        )
    }
}

/** A section's title in capitals, with an optional note on the right. */
@Composable
internal fun SectionTitle(title: String, note: String? = null) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp, bottom = 12.dp)
            .semantics { heading() },
    ) {
        Caption(title, color = LevitxColors.Text)
        note?.let { Caption(it, color = LevitxColors.TextFaint) }
    }
}
