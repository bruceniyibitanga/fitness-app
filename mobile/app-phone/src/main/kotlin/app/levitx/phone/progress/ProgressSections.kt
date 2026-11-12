package app.levitx.phone.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.CoachTuning
import app.levitx.core.history.PassedGate
import app.levitx.core.load.formatKg
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.measureText
import app.levitx.phone.ui.patternName
import app.levitx.phone.ui.shortDate
import app.levitx.phone.ui.signedKg

/** Workouts a week the grid has room for; more still count, they just fill the column. */
private const val WEEK_SQUARES = 4

/** Every movement trained in range, each opening its exercise's progress. */
@Composable
internal fun Moves(moves: List<MoveRow>, onOpenExercise: (String) -> Unit) {
    SectionTitle(stringResource(R.string.progress_moves), stringResource(R.string.figure_kg_per_rep))
    moves.forEach { move ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clickable(role = Role.Button, onClick = { onOpenExercise(move.exerciseId) })
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    patternName(move.pattern),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = LevitxColors.Text,
                )
                Caption(move.exerciseName, color = LevitxColors.TextFaint)
            }
            Text(
                text = move.kgNow?.let(::formatKg) ?: measureText(move.measure, move.best),
                style = LevitxType.Figure,
                color = LevitxColors.Text,
            )
            Text(
                text = move.changeKg?.let(::signedKg).orEmpty(),
                style = LevitxType.Caption,
                color = if ((move.changeKg ?: 0.0) > 0.0) LevitxColors.Accent else LevitxColors.TextMuted,
                modifier = Modifier
                    .width(56.dp)
                    .padding(start = 10.dp),
            )
        }
        Rule(Modifier.padding(horizontal = ScreenPadding))
    }
}

/**
 * Hard sets of each movement this week, as bars to the coach's limit, with a
 * tick at what it planned.
 */
@Composable
internal fun WeekSetsSection(weekSets: List<WeekSets>) {
    if (weekSets.isEmpty()) return
    val limit = CoachTuning.MAX_WEEKLY_HARD_SETS
    SectionTitle(
        stringResource(R.string.progress_week_sets),
        stringResource(R.string.progress_week_limit, limit.toInt()),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(horizontal = ScreenPadding)) {
        weekSets.forEach { WeekSetsRow(it, limit) }
        Text(
            text = stringResource(R.string.progress_week_note, limit.toInt()),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = LevitxColors.TextMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 22.dp),
        )
    }
    Rule()
}

/** One movement's bar: hard sets done against the limit, with a tick at the plan. */
@Composable
private fun WeekSetsRow(row: WeekSets, limit: Double) {
    val description = stringResource(
        R.string.progress_week_sets_description,
        patternName(row.pattern),
        formatKg(row.done),
        formatKg(row.planned),
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Text(
            text = patternName(row.pattern),
            fontSize = 13.sp,
            color = LevitxColors.TextSoft,
            modifier = Modifier.width(140.dp),
        )
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .background(LevitxColors.Track),
        ) {
            Box(
                Modifier
                    .fillMaxWidth((row.done / limit).toFloat().coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(LevitxColors.Text),
            )
            Box(
                Modifier
                    .offset(x = maxWidth * (row.planned / limit).toFloat().coerceIn(0f, 1f) - 1.dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(LevitxColors.Outline),
            )
        }
        Text(
            text = formatKg(row.done),
            style = LevitxType.Caption,
            color = LevitxColors.Text,
            modifier = Modifier
                .width(40.dp)
                .padding(start = 10.dp),
        )
    }
}

/** Workouts per week for twelve weeks, as columns of squares, and the way into every workout. */
@Composable
internal fun WeeksSection(weeks: List<Int>, onOpenHistory: () -> Unit) {
    val total = weeks.sum()
    SectionTitle(
        stringResource(R.string.progress_weeks),
        pluralStringResource(R.plurals.progress_weeks_total, total, total),
    )
    val description = stringResource(R.string.progress_weeks_description, weeks.joinToString())
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(horizontal = ScreenPadding)
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
    ) {
        weeks.forEach { count ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                for (level in WEEK_SQUARES downTo 1) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(if (count >= level) LevitxColors.Text else LevitxColors.Track),
                    )
                }
            }
        }
    }
    Row(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 8.dp)) {
        Caption(
            pluralStringResource(R.plurals.progress_weeks_ago, weeks.size, weeks.size),
            color = LevitxColors.TextFaint,
        )
        Spacer(Modifier.weight(1f))
        Caption(stringResource(R.string.progress_gap), color = LevitxColors.TextFaint)
        Spacer(Modifier.weight(1f))
        Caption(stringResource(R.string.progress_now), color = LevitxColors.TextFaint)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 18.dp)
            .fillMaxWidth()
            .height(56.dp)
            .clickable(role = Role.Button, onClick = onOpenHistory)
            .padding(horizontal = ScreenPadding),
    ) {
        Text(
            text = stringResource(R.string.progress_open_history),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = LevitxColors.Text,
            modifier = Modifier.weight(1f),
        )
        Icon(painterResource(R.drawable.ic_chevron_forward), contentDescription = null, tint = LevitxColors.TextFaint)
    }
    Rule()
}

/** Gates passed, with the path and the day. */
@Composable
internal fun GatesSection(gates: List<PassedGate>) {
    SectionTitle(stringResource(R.string.progress_gates))
    if (gates.isEmpty()) {
        Text(
            text = stringResource(R.string.progress_gates_none),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = LevitxColors.TextMuted,
            modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, bottom = 24.dp),
        )
        return
    }
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, bottom = 24.dp)) {
        gates.forEach { passed ->
            val path = SkillCatalogue.all.firstOrNull { passed.gate in it.gates }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics(mergeDescendants = true) {},
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(LevitxColors.Accent),
                )
                Column(
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .weight(1f),
                ) {
                    Text(passed.gate.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LevitxColors.Text)
                    path?.let { Caption(it.name, color = LevitxColors.TextFaint) }
                }
                passed.atEpochMillis?.let { Caption(shortDate(it), color = LevitxColors.TextMuted) }
            }
            Rule()
        }
    }
}
