package app.levitx.phone.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.PlanStyle
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.TrainingAim
import app.levitx.core.skills.SkillCatalogue
import app.levitx.core.skills.SkillLevel
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.CheckBadge
import app.levitx.phone.ui.ChoiceBar
import app.levitx.phone.ui.ChoiceRow
import app.levitx.phone.ui.ChoiceStyle
import app.levitx.phone.ui.PhotoCard
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.aimName
import app.levitx.phone.ui.pathPhoto
import app.levitx.phone.ui.programMeta

/*
 * The first questions of setup, each on its own screen as its artboard in
 * "Levitx App Design" (OnbGoal, OnbLevel, OnbAim, OnbPullUp): the goal, and
 * then three that are answered with one tap.
 */

private val GoalCardHeight = 132.dp

/**
 * What they are training for: the skills of one level at a time, as big
 * photo cards. The level tabs only change which skills are listed.
 */
@Composable
internal fun GoalStep(state: OnboardingUiState, actions: OnboardingActions) {
    val chosen = state.goalPathId?.let(SkillCatalogue::byId)
    // Opens on the level of the goal already picked, so coming back shows the pick.
    var level by rememberSaveable { mutableStateOf(chosen?.level ?: SkillLevel.entries.first()) }
    StepTitle(stringResource(R.string.goal_title))
    ChoiceRow(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 20.dp)) {
        SkillLevel.entries.forEach { each ->
            ChoiceBar(
                text = stringResource(levelTab(each)),
                selected = each == level,
                onClick = { level = each },
                style = ChoiceStyle.CREAM,
            )
        }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(start = ScreenPadding, end = ScreenPadding, top = 12.dp)
            .selectableGroup(),
    ) {
        SkillCatalogue.all.filter { it.level == level }.forEach { path ->
            val selected = state.goalChosen && state.goalPathId == path.id
            PhotoCard(
                photo = pathPhoto(path.id),
                title = path.name,
                caption = programMeta(path),
                selected = selected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GoalCardHeight)
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { actions.onChooseGoal(path.id) },
                    ),
            )
        }
    }
}

/** A level's name in sentence case, which fits a third of the screen where the caps used elsewhere don't. */
@StringRes
private fun levelTab(level: SkillLevel): Int = when (level) {
    SkillLevel.BEGINNER -> R.string.goal_level_beginner
    SkillLevel.INTERMEDIATE -> R.string.goal_level_intermediate
    SkillLevel.ADVANCED -> R.string.goal_level_advanced
}

/** "No skill in mind, just get fitter": general fitness, as a quiet line above Continue. */
@Composable
internal fun GeneralGoalLink(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Text(
            text = stringResource(R.string.goal_general_link),
            fontSize = 14.sp,
            color = if (selected) LevitxColors.Accent else LevitxColors.Text,
            textDecoration = if (selected) TextDecoration.None else TextDecoration.Underline,
        )
        if (selected) CheckBadge()
    }
}

/** All-round or focused. Only asked with a skill goal. */
@Composable
internal fun StyleStep(state: OnboardingUiState, actions: OnboardingActions) {
    StepTitle(stringResource(R.string.style_title))
    Options {
        OptionRow(
            title = stringResource(R.string.how_all_round),
            caption = stringResource(R.string.how_all_round_body),
            selected = state.style == PlanStyle.ALL_ROUND,
            onClick = { actions.onChooseStyle(PlanStyle.ALL_ROUND) },
        )
        OptionRow(
            title = stringResource(R.string.how_focused),
            caption = stringResource(R.string.how_focused_body),
            selected = state.style == PlanStyle.FOCUSED,
            onClick = { actions.onChooseStyle(PlanStyle.FOCUSED) },
        )
    }
}

/** What the training is tuned for, under the same four names the Body tab uses. */
@Composable
internal fun AimStep(state: OnboardingUiState, actions: OnboardingActions) {
    StepTitle(stringResource(R.string.aim_title))
    Options {
        TrainingAim.entries.forEach { aim ->
            OptionRow(
                title = stringResource(aimName(aim)),
                caption = stringResource(aimShort(aim)),
                selected = state.aim == aim,
                onClick = { actions.onChooseAim(aim) },
            )
        }
    }
}

/** A few words on what [aim] means, short enough for an answer row. */
@StringRes
private fun aimShort(aim: TrainingAim): Int = when (aim) {
    TrainingAim.BALANCED -> R.string.aim_balanced_short
    TrainingAim.MUSCLE -> R.string.aim_muscle_short
    TrainingAim.STRENGTH -> R.string.aim_strength_short
    TrainingAim.CONDITIONING -> R.string.aim_conditioning_short
}

/** "Can you do a strict pull-up?": the one thing the first sessions can't safely find out. */
@Composable
internal fun PullUpStep(state: OnboardingUiState, actions: OnboardingActions) {
    StepTitle(stringResource(R.string.how_pull_up_label))
    Options {
        listOf(
            PullUpAnswer.YES to R.string.how_pull_up_yes,
            PullUpAnswer.NOT_YET to R.string.how_pull_up_not_yet,
            PullUpAnswer.NOT_SURE to R.string.how_pull_up_not_sure,
        ).forEach { (answer, label) ->
            OptionRow(
                title = stringResource(label),
                selected = state.pullUp == answer,
                onClick = { actions.onAnswerPullUp(answer) },
            )
        }
    }
    StepNote(stringResource(R.string.how_pull_up_note))
}
