package app.levitx.phone.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.levitx.core.coach.ExercisePlan
import app.levitx.core.coach.PlannedSession
import app.levitx.core.load.calculateEffectiveLoad
import app.levitx.core.load.formatKg
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import kotlin.math.roundToInt

/**
 * The session, one row per exercise, as on the Today artboard: number, name,
 * sets and target, and on the right the load per rep when bodyweight is known,
 * its share of bodyweight when it isn't, or the hold time. Shared by Today,
 * the Train tab and setup, so a session looks the same wherever it was planned.
 *
 * @param onExerciseClick Opens an exercise by id, or null when rows aren't tappable.
 */
@Composable
fun SessionList(session: PlannedSession, bodyweightKg: Double?, onExerciseClick: ((String) -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(horizontal = ScreenPadding),
    ) {
        Caption(
            pluralStringResource(R.plurals.today_exercises, session.plans.size, session.plans.size),
            modifier = Modifier.weight(1f),
        )
        Caption(stringResource(R.string.today_minutes, session.estimatedMinutes), color = LevitxColors.TextFaint)
    }
    Rule()
    session.plans.forEachIndexed { index, plan ->
        val figure = planFigure(plan, bodyweightKg)
        NumberedRow(
            number = index + 1,
            title = plan.exercise.name,
            detail = planDetail(plan),
            figure = figure?.first,
            figureCaption = figure?.second,
            figureColor = if (plan.exercise.measure ==
                GateMeasure.HOLD_SECONDS
            ) {
                LevitxColors.Ice
            } else {
                LevitxColors.Text
            },
            highlighted = index == 0,
            onClick = onExerciseClick?.let { open -> { open(plan.exercise.id) } },
        )
    }
}

/** "2 × 6 reps, then a best effort", with "skill" or "light" after it when it is. */
@Composable
fun planDetail(plan: ExercisePlan): String {
    val target = measureText(plan.exercise.measure, plan.workTarget)
    val hasBestEffort = plan.sets.last().isBestEffort && plan.sets.size > 1
    val workSets = if (hasBestEffort) plan.sets.size - 1 else plan.sets.size
    val sets = if (hasBestEffort) {
        stringResource(R.string.today_sets_then_best, workSets, target)
    } else {
        stringResource(R.string.today_sets, workSets, target)
    }
    return when {
        !plan.isHard -> stringResource(R.string.plan_detail_tagged, sets, stringResource(R.string.plan_tag_light))

        plan.gateId != null -> stringResource(
            R.string.plan_detail_tagged,
            sets,
            stringResource(R.string.plan_tag_skill),
        )

        else -> sets
    }
}

/**
 * The figure at the end of a row and its caption: "31 kg / KG PER REP" with
 * bodyweight, "41% / OF BODYWEIGHT" without, "20 s / HOLD" for holds, or null
 * when there is nothing measured to show.
 */
@Composable
private fun planFigure(plan: ExercisePlan, bodyweightKg: Double?): Pair<String, String>? {
    val profile = plan.exercise.loadProfile
    return when {
        profile != null && bodyweightKg != null -> stringResource(
            R.string.kg_per_rep,
            formatKg(calculateEffectiveLoad(bodyweightKg, profile).totalKg),
        ) to stringResource(R.string.figure_kg_per_rep)

        profile != null -> stringResource(R.string.percent, (profile.bodyweightShare * PERCENT).roundToInt()) to
            stringResource(R.string.figure_of_bodyweight)

        plan.exercise.measure == GateMeasure.HOLD_SECONDS ->
            measureText(GateMeasure.HOLD_SECONDS, plan.workTarget) to stringResource(R.string.figure_hold)

        else -> null
    }
}

private const val PERCENT = 100
