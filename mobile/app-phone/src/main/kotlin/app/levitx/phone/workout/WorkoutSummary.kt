package app.levitx.phone.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.StatStrip
import app.levitx.phone.ui.TabHeader
import app.levitx.phone.ui.measureText

/**
 * The workout is saved: how much was done, new bests, every set, and what
 * the coach changed for next time, so the person sees their work count.
 */
@Composable
internal fun ColumnScope.Summary(report: SessionReport, onDone: () -> Unit) {
    TabHeader(stringResource(R.string.workout_saved_label))
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
    ) {
        Headline(report)
        if (report.bests.isNotEmpty()) Bests(report.bests)
        if (report.exercises.isNotEmpty()) EverySet(report.exercises)
        if (report.changes.isNotEmpty()) Changes(report.changes)
    }
    AccentBar(text = stringResource(R.string.workout_done), onClick = onDone, height = BottomBarHeight)
}

/** The sets done, huge, and the figures under them. */
@Composable
private fun Headline(report: SessionReport) {
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp, bottom = 20.dp)) {
        Caption(
            text = if (report.bests.isEmpty()) {
                stringResource(R.string.workout_summary_done)
            } else {
                pluralStringResource(R.plurals.workout_summary_bests, report.bests.size, report.bests.size)
            },
            color = LevitxColors.Accent,
        )
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 14.dp)) {
            Text(
                text = report.sets.toString(),
                style = LevitxType.Hero.copy(fontSize = 124.sp, lineHeight = 100.sp),
                color = LevitxColors.Text,
            )
            Text(
                text = pluralStringResource(R.plurals.workout_summary_sets, report.sets),
                style = LevitxType.Poster,
                color = LevitxColors.Text,
                modifier = Modifier
                    .padding(start = 12.dp, bottom = 6.dp)
                    .semantics { heading() },
            )
        }
    }
    Rule()
    StatStrip(
        figures = listOfNotNull(
            report.kgMoved?.let {
                stringResource(R.string.workout_summary_kg, it) to
                    stringResource(R.string.workout_summary_kg_label)
            },
            stringResource(R.string.workout_summary_minutes, report.minutes) to
                stringResource(R.string.workout_summary_time_label),
            report.exercises.size.toString() to stringResource(R.string.workout_summary_exercises_label),
        ),
        modifier = Modifier.padding(horizontal = ScreenPadding),
    )
    Rule()
}

/** New bests: the new figure in orange, and what it was. */
@Composable
private fun Bests(bests: List<ReportBest>) {
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 18.dp)) {
        Caption(stringResource(R.string.workout_summary_bests_label), modifier = Modifier.semantics { heading() })
        bests.forEach { best ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
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
                    Text(best.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LevitxColors.Text)
                    Caption(
                        text = when {
                            best.passedGate -> stringResource(R.string.workout_summary_gate_passed)

                            best.was != null -> stringResource(
                                R.string.workout_summary_was,
                                measureText(best.measure, best.was),
                            )

                            else -> stringResource(R.string.workout_summary_first)
                        },
                        color = if (best.passedGate) LevitxColors.Accent else LevitxColors.TextFaint,
                    )
                }
                Text(measureText(best.measure, best.value), style = LevitxType.Figure, color = LevitxColors.Accent)
            }
            Rule()
        }
    }
}

/** Every exercise with its sets as small squares: the figure, and reps left under it. */
@Composable
private fun EverySet(exercises: List<ReportExercise>) {
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 18.dp)) {
        Caption(stringResource(R.string.workout_summary_sets_label), modifier = Modifier.semantics { heading() })
        exercises.forEach { exercise ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
            ) {
                Text(
                    text = exercise.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = LevitxColors.Text,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    exercise.sets.forEach { SetSquare(exercise.measure, it) }
                }
            }
            Rule()
        }
    }
}

/** One set: "8" over "2" (reps left), or "30s" over a dot for a hold. */
@Composable
private fun SetSquare(measure: GateMeasure, set: LoggedSet) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(38.dp)
            .border(1.dp, LevitxColors.Tick),
    ) {
        Text(
            text = if (measure == GateMeasure.HOLD_SECONDS) {
                stringResource(R.string.workout_summary_seconds, set.value)
            } else {
                set.value.toString()
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = LevitxColors.Text,
        )
        Text(
            text = set.repsInReserve?.let { repsLeftLabel(it) } ?: stringResource(R.string.workout_summary_no_left),
            style = LevitxType.Micro,
            color = LevitxColors.TextFaint,
        )
    }
}

/** What the coach changed for next time, one line an exercise. */
@Composable
private fun Changes(changes: List<ReportChange>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(start = ScreenPadding, end = ScreenPadding, top = 18.dp, bottom = 20.dp)
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .border(1.dp, LevitxColors.Line)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Caption(
            stringResource(R.string.workout_summary_changes_label),
            color = LevitxColors.Ice,
            modifier = Modifier.semantics { heading() },
        )
        changes.forEach { change ->
            val detail = changeText(change)
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = LevitxColors.Text)) {
                        append(change.name)
                    }
                    append(' ')
                    append(detail)
                },
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = LevitxColors.TextSoft,
            )
        }
    }
}

/** The rest of a change's sentence, after the exercise's name. */
@Composable
private fun changeText(change: ReportChange): String = when (change) {
    is ReportChange.Target -> stringResource(
        R.string.workout_change_target,
        measureText(change.measure, change.from),
        measureText(change.measure, change.to),
    )

    is ReportChange.Held -> stringResource(R.string.workout_change_held, measureText(change.measure, change.target))

    is ReportChange.MovedUp -> stringResource(R.string.workout_change_up, change.next)

    is ReportChange.MovedDown -> stringResource(R.string.workout_change_down, change.next)
}
