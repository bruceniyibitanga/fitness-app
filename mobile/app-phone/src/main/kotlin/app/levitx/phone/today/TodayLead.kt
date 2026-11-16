package app.levitx.phone.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.ExercisePlan
import app.levitx.core.load.calculateEffectiveLoad
import app.levitx.core.load.formatKg
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.measureText
import app.levitx.phone.ui.patternName
import kotlin.math.roundToInt

private const val PERCENT = 100

/**
 * The first exercise, big: "3×6" and its load per rep side by side, the
 * exercise under them, and how hard to go.
 */
@Composable
internal fun Lead(plan: ExercisePlan, bodyweightKg: Double?) {
    val hasBestEffort = plan.sets.any { it.isBestEffort } && plan.sets.size > 1
    val workSets = plan.sets.count { !it.isBestEffort }.coerceAtLeast(1)
    val profile = plan.exercise.loadProfile
    val isHold = plan.exercise.measure == GateMeasure.HOLD_SECONDS
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 24.dp, bottom = 22.dp)) {
        Caption(
            when {
                plan.isGateAttempt -> stringResource(R.string.today_lead_attempt, patternName(plan.exercise.pattern))
                plan.gateId != null -> stringResource(R.string.today_lead_skill, patternName(plan.exercise.pattern))
                else -> stringResource(R.string.today_lead_first, patternName(plan.exercise.pattern))
            },
        )
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 14.dp)) {
            Text(
                text =
                    stringResource(R.string.today_hero, workSets, plan.workTarget) +
                        if (isHold) stringResource(R.string.seconds_unit) else "",
                style = LevitxType.Hero,
                color = LevitxColors.Text,
                modifier = Modifier.weight(1f),
            )
            when {
                profile != null && bodyweightKg != null -> LeadFigure(
                    formatKg(calculateEffectiveLoad(bodyweightKg, profile).totalKg),
                    stringResource(R.string.figure_kg_per_rep),
                )

                profile != null -> LeadFigure(
                    stringResource(R.string.percent, (profile.bodyweightShare * PERCENT).roundToInt()),
                    stringResource(R.string.figure_of_bodyweight),
                )
            }
        }
        Text(
            text = plan.exercise.name,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = LevitxColors.Text,
            modifier = Modifier
                .padding(top = 18.dp)
                .semantics { heading() },
        )
        Text(
            text = leadNote(plan, isHold, hasBestEffort),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = LevitxColors.TextMuted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** How hard to go on the first exercise, and what is special about its sets. */
@Composable
private fun leadNote(plan: ExercisePlan, isHold: Boolean, hasBestEffort: Boolean): String {
    val effort = if (isHold) {
        stringResource(R.string.today_lead_note_hold)
    } else {
        pluralStringResource(R.plurals.today_lead_note_reps, plan.repsInReserve, plan.repsInReserve)
    }
    val sets = when {
        plan.isGateAttempt -> stringResource(
            R.string.today_lead_note_attempt,
            measureText(plan.exercise.measure, plan.sets.first().target),
        )

        hasBestEffort -> stringResource(R.string.today_lead_note_best)

        else -> null
    }
    return listOfNotNull(effort, sets).joinToString(" ")
}

/** The load per rep beside the big number, in the accent colour. */
@Composable
private fun LeadFigure(value: String, caption: String) {
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 4.dp)) {
        Text(
            text = value,
            style = LevitxType.Figure.copy(fontSize = 34.sp, lineHeight = 34.sp),
            color = LevitxColors.Accent,
        )
        Text(
            text = caption,
            style = LevitxType.Micro,
            color = LevitxColors.TextMuted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
