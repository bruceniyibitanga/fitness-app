package app.levitx.phone.body

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.WeighIn
import app.levitx.core.load.formatKg
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Help
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/*
 * The parts of the Body tab, sized as on the "Body and load" artboard.
 */

private val ChartHeight = 110.dp
private val DotSize = 4.dp
private val EndMarkSize = 8.dp
private val LineWidth = 2.dp
private val HeaderRowHeight = 40.dp
private val LoadRowHeight = 64.dp

/** Readings either side of each point that the drawn line averages over, to calm daily swings. */
private const val SMOOTHING_READINGS = 1

/** Kilograms of headroom above and below the readings, so the line never touches the edges. */
private const val CHART_PADDING_KG = 0.5

private const val PERCENT = 100

/** "BODYWEIGHT · TREND", the weight as one big number with its weekly change, and the chart. */
@Composable
internal fun BodyweightTrend(state: BodyUiState.Ready) {
    val trend = state.trend
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp)) {
        Caption(stringResource(R.string.body_trend_label))
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 12.dp)) {
            Text(
                text = trend?.let { formatKg(it.currentKg) } ?: stringResource(R.string.no_value),
                style = LevitxType.Hero.copy(fontSize = 96.sp, lineHeight = 80.sp),
                color = if (trend == null) LevitxColors.TextFaint else LevitxColors.Text,
            )
            Caption(stringResource(R.string.kg), modifier = Modifier.padding(start = 8.dp, bottom = 6.dp).weight(1f))
            trend?.weeklyChange?.let { change ->
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(signedKg(change * trend.currentKg), style = LevitxType.Figure, color = LevitxColors.Text)
                    Text(
                        stringResource(R.string.body_kg_per_week),
                        style = LevitxType.Micro,
                        color = LevitxColors.TextMuted,
                    )
                }
            }
        }
        if (state.weighIns.size >= 2) {
            TrendChart(state.weighIns, state.nowEpochMillis, trend?.currentKg ?: state.weighIns.last().kg)
        } else {
            Help(
                text = stringResource(if (trend == null) R.string.body_no_weight else R.string.body_trend_waiting),
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

/**
 * The readings as small squares, a line through them averaged over their
 * neighbours, and an orange square where it ends. Time runs left to right
 * from the first reading to now.
 */
@Composable
private fun TrendChart(weighIns: List<WeighIn>, nowEpochMillis: Long, currentKg: Double) {
    val description = stringResource(R.string.body_chart_description, formatKg(currentKg))
    val start = weighIns.first().atEpochMillis
    val low = weighIns.minOf { it.kg } - CHART_PADDING_KG
    val high = weighIns.maxOf { it.kg } + CHART_PADDING_KG
    val smoothed = remember(weighIns) { smooth(weighIns) }
    Canvas(
        modifier = Modifier
            .padding(top = 20.dp)
            .fillMaxWidth()
            .height(ChartHeight)
            .semantics { contentDescription = description },
    ) {
        val span = (nowEpochMillis - start).coerceAtLeast(1L).toFloat()
        fun x(at: Long) = (at - start) / span * size.width
        fun y(kg: Double) = ((high - kg) / (high - low)).toFloat() * size.height
        drawLine(LevitxColors.Line, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f))
        drawLine(LevitxColors.Track, Offset(0f, size.height / 2), Offset(size.width, size.height / 2))
        val dot = DotSize.toPx()
        weighIns.forEach {
            drawRect(LevitxColors.TextFaint, Offset(x(it.atEpochMillis) - dot / 2, y(it.kg) - dot / 2), Size(dot, dot))
        }
        val line = Path()
        smoothed.forEachIndexed { index, point ->
            val at = Offset(x(point.atEpochMillis), y(point.kg))
            if (index == 0) line.moveTo(at.x, at.y) else line.lineTo(at.x, at.y)
        }
        drawPath(line, LevitxColors.Text, style = Stroke(width = LineWidth.toPx(), cap = StrokeCap.Square))
        val end = smoothed.last()
        val mark = EndMarkSize.toPx()
        drawRect(
            LevitxColors.Accent,
            Offset(x(end.atEpochMillis) - mark / 2, y(end.kg) - mark / 2),
            Size(mark, mark),
        )
    }
    Row(modifier = Modifier.padding(top = 6.dp)) {
        Text(
            text = shortDate(start),
            style = LevitxType.Micro,
            color = LevitxColors.TextFaint,
            modifier = Modifier.weight(1f),
        )
        Text(stringResource(R.string.body_today), style = LevitxType.Micro, color = LevitxColors.TextFaint)
    }
}

/** Each reading averaged with its neighbours. A day's weight swings with food and water. */
private fun smooth(weighIns: List<WeighIn>): List<WeighIn> = weighIns.indices.map { index ->
    val window = weighIns.subList(
        (index - SMOOTHING_READINGS).coerceAtLeast(0),
        (index + SMOOTHING_READINGS + 1).coerceAtMost(weighIns.size),
    )
    WeighIn(weighIns[index].atEpochMillis, window.map { it.kg }.average())
}

/** "13 OCT", in the phone's own time zone and language. */
@Composable
private fun shortDate(epochMillis: Long): String {
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM") }
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(formatter).uppercase()
}

/** "−0.3" or "+0.2": a change in kg with its sign, using a real minus. */
private fun signedKg(kg: Double): String = when {
    kg < 0 -> "−" + formatKg(-kg)
    kg > 0 -> "+" + formatKg(kg)
    else -> formatKg(0.0)
}

/**
 * "ON THE BAR": each exercise trained now, the share of bodyweight it moves,
 * its kilograms per rep, and how that is changing a week.
 */
@Composable
internal fun OnTheBar(state: BodyUiState.Ready) {
    if (state.onTheBar.isEmpty()) return
    Rule(Modifier.padding(top = 18.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(HeaderRowHeight)
            .padding(horizontal = ScreenPadding),
    ) {
        Caption(stringResource(R.string.body_on_the_bar), modifier = Modifier.weight(1f))
        Caption(stringResource(R.string.body_kg_per_rep), color = LevitxColors.TextFaint)
    }
    Rule()
    state.onTheBar.forEach { load ->
        LoadRow(load)
        Rule()
    }
    state.trend?.let { trend ->
        Help(
            text = stringResource(R.string.body_kilo_note, formatKg(PERCENT / trend.currentKg)),
            modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 14.dp),
        )
        Rule()
    }
}

@Composable
private fun LoadRow(load: BarLoad) {
    val share = (load.bodyweightShare * PERCENT).roundToInt()
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LoadRowHeight)
            .padding(horizontal = ScreenPadding, vertical = 8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(load.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LevitxColors.Text)
            Text(
                text = stringResource(
                    if (load.measured) R.string.body_share_measured else R.string.body_share_estimated,
                    share,
                ),
                style = LevitxType.Micro,
                color = LevitxColors.TextFaint,
            )
        }
        Text(formatKg(load.kgPerRep), style = LevitxType.Figure, color = LevitxColors.Text)
        Text(
            text = load.kgPerWeek?.let(::signedKg).orEmpty(),
            style = LevitxType.Micro,
            color = LevitxColors.Accent,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp),
        )
    }
}
