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
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.phone.R

/**
 * The session, one row per exercise: number, name, and on the right the sets
 * and target ("3 × 6"), with a word under it when the work is skill work,
 * light, or a gate test. One line each, so the whole session reads at a
 * glance; the cues, the load per rep and the best-effort set are on the
 * exercise's own page and in the set itself. Shared by Today, the Train tab
 * and setup, so a session looks the same wherever it was planned.
 *
 * @param onExerciseClick Opens an exercise by id, or null when rows aren't tappable.
 * @param heading The caption over the rows, with the session's length beside
 *   it, or null where the screen has already said both.
 */
@Composable
fun SessionList(
    session: PlannedSession,
    onExerciseClick: ((String) -> Unit)? = null,
    heading: String? = pluralStringResource(R.plurals.today_exercises, session.plans.size, session.plans.size),
) {
    if (heading != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(horizontal = ScreenPadding),
        ) {
            Caption(heading, modifier = Modifier.weight(1f))
            Caption(stringResource(R.string.today_minutes, session.estimatedMinutes), color = LevitxColors.TextFaint)
        }
    }
    Rule()
    session.plans.forEachIndexed { index, plan ->
        val isHold = plan.exercise.measure == GateMeasure.HOLD_SECONDS
        NumberedRow(
            number = index + 1,
            title = plan.exercise.name,
            figure = planSets(plan),
            figureCaption = planTag(plan),
            figureColor = if (isHold) LevitxColors.Ice else LevitxColors.Text,
            onClick = onExerciseClick?.let { open -> { open(plan.exercise.id) } },
        )
    }
}

/** "3 × 6", or "3 × 5 s" for holds: the work sets and their target, without the best-effort set. */
@Composable
private fun planSets(plan: ExercisePlan): String = stringResource(
    R.string.today_sets,
    plan.sets.count { !it.isBestEffort }.coerceAtLeast(1),
    shortTarget(plan.exercise.measure, plan.workTarget),
)

/** "6", or "5 s" for a hold: a target short enough to sit after "3 ×". */
@Composable
private fun shortTarget(measure: GateMeasure, value: Int): String =
    if (measure == GateMeasure.HOLD_SECONDS) measureText(measure, value) else value.toString()

/** The one word that says what is special about [plan]'s work, or null for ordinary hard work. */
@Composable
private fun planTag(plan: ExercisePlan): String? = when {
    plan.isGateAttempt -> stringResource(R.string.plan_tag_test)
    !plan.isHard -> stringResource(R.string.plan_tag_light)
    plan.gateId != null -> stringResource(R.string.plan_tag_skill)
    else -> null
}
