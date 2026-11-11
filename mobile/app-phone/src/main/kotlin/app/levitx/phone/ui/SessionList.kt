package app.levitx.phone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.levitx.core.coach.ExercisePlan
import app.levitx.core.coach.PlannedSession
import app.levitx.core.load.calculateEffectiveLoad
import app.levitx.core.load.formatKg
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R

private val NumberColumnWidth = 28.dp

/** Material's minimum touch target, for when rows can be tapped. */
private val RowMinHeight = 48.dp

/**
 * The session, one row per exercise: sets, target, load per rep when
 * bodyweight is known, and a tag for light or skill work. Shared by Today and
 * the Train tab, so a session looks the same wherever it was planned.
 *
 * @param onExerciseClick Opens an exercise by id, or null when rows aren't tappable.
 */
@Composable
fun SessionList(session: PlannedSession, bodyweightKg: Double?, onExerciseClick: ((String) -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(
            pluralStringResource(R.plurals.today_exercises, session.plans.size, session.plans.size),
            modifier = Modifier.weight(1f),
        )
        SectionLabel(stringResource(R.string.today_minutes, session.estimatedMinutes))
    }
    HorizontalDivider(color = LevitxColors.Line)
    session.plans.forEachIndexed { index, plan ->
        ExerciseRow(index + 1, plan, bodyweightKg, onExerciseClick)
        HorizontalDivider(color = LevitxColors.Line)
    }
}

@Composable
private fun ExerciseRow(number: Int, plan: ExercisePlan, bodyweightKg: Double?, onClick: ((String) -> Unit)?) {
    val target = measureText(plan.exercise.measure, plan.workTarget)
    val hasBestEffort = plan.sets.last().isBestEffort && plan.sets.size > 1
    val workSets = if (hasBestEffort) plan.sets.size - 1 else plan.sets.size
    val openLabel = stringResource(R.string.open_exercise, plan.exercise.name)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(onClickLabel = openLabel, role = Role.Button) { onClick(plan.exercise.id) }
                },
            )
            .heightIn(min = RowMinHeight)
            .padding(vertical = LevitxSpacing.Small),
    ) {
        Text(
            text = number.toString().padStart(2, '0'),
            style = LevitxType.Label,
            color = if (number == 1) LevitxColors.Accent else LevitxColors.TextFaint,
            modifier = Modifier.width(NumberColumnWidth),
        )
        Column(verticalArrangement = Arrangement.spacedBy(LevitxSpacing.XSmall), modifier = Modifier.weight(1f)) {
            Text(plan.exercise.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (hasBestEffort) {
                    stringResource(R.string.today_sets_then_best, workSets, target)
                } else {
                    stringResource(R.string.today_sets, workSets, target)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = LevitxColors.TextMuted,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            val profile = plan.exercise.loadProfile
            if (profile != null && bodyweightKg != null) {
                Text(
                    text = stringResource(
                        R.string.kg_per_rep,
                        formatKg(calculateEffectiveLoad(bodyweightKg, profile).totalKg),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            when {
                !plan.isHard -> SectionLabel(stringResource(R.string.today_light), color = LevitxColors.Ice)
                plan.gateId != null -> SectionLabel(stringResource(R.string.today_skill), color = LevitxColors.Accent)
            }
        }
    }
}
