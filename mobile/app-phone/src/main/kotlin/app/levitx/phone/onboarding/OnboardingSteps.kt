package app.levitx.phone.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PlanStyle
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.ChoiceButton
import app.levitx.phone.ui.OptionCard
import app.levitx.phone.ui.SectionLabel
import app.levitx.phone.ui.ToggleChip
import app.levitx.phone.ui.kitName
import app.levitx.phone.ui.measureText
import java.time.DayOfWeek
import java.time.format.TextStyle

/*
 * The steps of setup, one composable each. The screen around them, with the
 * progress bar and the main button, is in OnboardingScreen.kt.
 */

/** Kit chips side by side. Two fit the longest name at any font size. */
private const val KIT_PER_ROW = 2

/** What they are training for: a skill path or general fitness. */
@Composable
internal fun GoalStep(state: OnboardingUiState, actions: OnboardingActions) {
    Title(stringResource(R.string.goal_title))
    Body(stringResource(R.string.goal_body))
    SectionLabel(stringResource(R.string.goal_skill_label))
    SkillCatalogue.all.forEach { path ->
        OptionCard(
            title = path.name,
            body = pluralStringResource(R.plurals.goal_skill_gates, path.gates.size, path.gates.size),
            selected = state.goalChosen && state.goalPathId == path.id,
            onClick = { actions.onChooseGoal(path.id) },
        )
    }
    Text(
        text = stringResource(R.string.goal_more_coming),
        style = MaterialTheme.typography.bodySmall,
        color = LevitxColors.TextMuted,
    )
    SectionLabel(stringResource(R.string.goal_general_label))
    OptionCard(
        title = stringResource(R.string.goal_general),
        body = stringResource(R.string.goal_general_detail),
        selected = state.goalChosen && state.goalPathId == null,
        onClick = { actions.onChooseGoal(null) },
    )
}

/** All-round or focused, whether they can do a pull-up, and bodyweight if they want to give it. */
@Composable
internal fun HowStep(state: OnboardingUiState, actions: OnboardingActions) {
    Title(stringResource(R.string.how_title))
    if (state.goalPathId != null) {
        SectionLabel(stringResource(R.string.how_plan_label))
        OptionCard(
            title = stringResource(R.string.how_all_round),
            body = stringResource(R.string.how_all_round_body),
            selected = state.style == PlanStyle.ALL_ROUND,
            onClick = { actions.onChooseStyle(PlanStyle.ALL_ROUND) },
            badge = stringResource(R.string.how_suggested),
        )
        OptionCard(
            title = stringResource(R.string.how_focused),
            body = stringResource(R.string.how_focused_body),
            selected = state.style == PlanStyle.FOCUSED,
            onClick = { actions.onChooseStyle(PlanStyle.FOCUSED) },
        )
    }
    SectionLabel(stringResource(R.string.how_pull_up_label))
    Row(horizontalArrangement = Arrangement.spacedBy(LevitxSpacing.Small), modifier = Modifier.fillMaxWidth()) {
        ChoiceButton(stringResource(R.string.how_pull_up_yes), state.pullUp == PullUpAnswer.YES) {
            actions.onAnswerPullUp(PullUpAnswer.YES)
        }
        ChoiceButton(stringResource(R.string.how_pull_up_not_yet), state.pullUp == PullUpAnswer.NOT_YET) {
            actions.onAnswerPullUp(PullUpAnswer.NOT_YET)
        }
        ChoiceButton(stringResource(R.string.how_pull_up_not_sure), state.pullUp == PullUpAnswer.NOT_SURE) {
            actions.onAnswerPullUp(PullUpAnswer.NOT_SURE)
        }
    }
    Note(stringResource(R.string.how_pull_up_note))
    SectionLabel(stringResource(R.string.how_bodyweight_label))
    OutlinedTextField(
        value = state.bodyweightText,
        onValueChange = actions.onTypeBodyweight,
        placeholder = { Text(stringResource(R.string.how_bodyweight_hint)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        isError = state.bodyweightInvalid,
        supportingText = if (state.bodyweightInvalid) {
            { Text(stringResource(R.string.how_bodyweight_invalid)) }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Note(stringResource(R.string.how_bodyweight_note))
}

/** Training days, time per session and kit. */
@Composable
internal fun WeekStep(state: OnboardingUiState, actions: OnboardingActions) {
    Title(stringResource(R.string.week_title))
    Body(stringResource(R.string.week_body))
    Row(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.week_days_label), modifier = Modifier.weight(1f))
        SectionLabel(
            pluralStringResource(R.plurals.week_days_count, state.days.size, state.days.size),
            color = LevitxColors.Accent,
        )
    }
    // Read through the configuration so the day letters follow a language change.
    val locale = LocalConfiguration.current.locales[0]
    Row(horizontalArrangement = Arrangement.spacedBy(LevitxSpacing.XSmall), modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            ToggleChip(
                text = day.getDisplayName(TextStyle.NARROW, locale),
                checked = day in state.days,
                onCheckedChange = { actions.onToggleDay(day) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    SectionLabel(stringResource(R.string.week_minutes_label))
    Row(horizontalArrangement = Arrangement.spacedBy(LevitxSpacing.Small), modifier = Modifier.fillMaxWidth()) {
        SessionLengths.forEach { minutes ->
            ChoiceButton(stringResource(R.string.minutes_short, minutes), state.minutes == minutes) {
                actions.onChooseMinutes(minutes)
            }
        }
    }
    SectionLabel(stringResource(R.string.week_kit_label))
    Equipment.entries.chunked(KIT_PER_ROW).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(LevitxSpacing.Small), modifier = Modifier.fillMaxWidth()) {
            row.forEach { kit ->
                ToggleChip(
                    text = kitName(kit),
                    checked = kit in state.equipment,
                    onCheckedChange = { actions.onToggleEquipment(kit) },
                    modifier = Modifier.weight(1f),
                )
            }
            // Keeps a lone last chip the same width as the others.
            repeat(KIT_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    Note(stringResource(R.string.week_kit_note))
}

/** The plan in brief, with the first session the coach would plan. */
@Composable
internal fun ReadyStep(state: OnboardingUiState) {
    SectionLabel(stringResource(R.string.ready_label), color = LevitxColors.Accent)
    Title(state.goalPathId?.let { SkillCatalogue.byId(it)?.name } ?: stringResource(R.string.ready_general))
    Row(horizontalArrangement = Arrangement.spacedBy(LevitxSpacing.Large)) {
        Figure(state.days.size.toString(), stringResource(R.string.ready_per_week))
        Figure(state.minutes.toString(), stringResource(R.string.ready_minutes))
        if (state.goalPathId != null) {
            val style = when (state.style) {
                PlanStyle.FOCUSED -> R.string.ready_style_focused
                PlanStyle.ALL_ROUND -> R.string.ready_style_all_round
            }
            Figure("", stringResource(style))
        }
    }
    HorizontalDivider(color = LevitxColors.Line)
    state.firstSession?.let { FirstSession(it) }
    Note(stringResource(R.string.ready_learning))
}

@Composable
private fun Figure(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(LevitxSpacing.XSmall)) {
        if (value.isNotEmpty()) Text(value, style = LevitxType.Numeral)
        SectionLabel(label)
    }
}

@Composable
private fun FirstSession(session: PlannedSession) {
    SectionLabel(stringResource(R.string.ready_first_session))
    session.plans.forEach { plan ->
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(plan.exercise.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                text = stringResource(
                    R.string.today_sets,
                    plan.sets.size,
                    measureText(plan.exercise.measure, plan.workTarget),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = LevitxColors.TextMuted,
            )
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = LevitxColors.TextMuted)
}
