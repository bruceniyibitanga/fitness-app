package app.levitx.phone.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.CoachPhrases
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PlanStyle
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.ChoiceBar
import app.levitx.phone.ui.ChoiceRow
import app.levitx.phone.ui.ChoiceStyle
import app.levitx.phone.ui.Help
import app.levitx.phone.ui.PhotoTile
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.SessionList
import app.levitx.phone.ui.SquareNote
import app.levitx.phone.ui.StatStrip
import app.levitx.phone.ui.kitName
import java.time.DayOfWeek
import java.time.format.TextStyle

/*
 * The setup steps after the welcome, each laid out as its artboard in
 * "Levitx App Design" (OnbGoal, OnbLevel, OnbSchedule, OnbPlan).
 */

/** Kit in the order setup offers it: the most useful first. */
private val KitOrder = listOf(
    Equipment.PULL_UP_BAR,
    Equipment.DIP_BARS,
    Equipment.RINGS,
    Equipment.LOW_BAR,
    Equipment.BAND,
    Equipment.BOX,
)

/** Kit choices side by side. Two fit the longest name at any font size. */
private const val KIT_PER_ROW = 2

/** A photo for each skill path that has one. */
private val GoalPhotos = mapOf(
    "first_pull_up" to R.drawable.photo_goal_first_pull_up,
    "muscle_up" to R.drawable.photo_goal_muscle_up,
)

/** What they are training for: a skill, as photo tiles, or general fitness. */
@Composable
internal fun GoalStep(state: OnboardingUiState, actions: OnboardingActions) {
    StepTitle(stringResource(R.string.goal_title), stringResource(R.string.goal_body))
    Question(stringResource(R.string.goal_skill_label), asCaption = true) {
        SkillCatalogue.all.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { path ->
                    PhotoTile(
                        photo = GoalPhotos[path.id] ?: R.drawable.photo_goal_first_pull_up,
                        title = path.name,
                        caption = pluralStringResource(R.plurals.goal_skill_gates, path.gates.size, path.gates.size),
                        selected = state.goalChosen && state.goalPathId == path.id,
                        onClick = { actions.onChooseGoal(path.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Help(stringResource(R.string.goal_more_coming))
    }
    Question(stringResource(R.string.goal_general_label), asCaption = true) {
        Column {
            Rule()
            GeneralGoalRow(
                title = stringResource(R.string.goal_general),
                tag = stringResource(R.string.goal_general_tag),
                selected = state.goalChosen && state.goalPathId == null,
                onClick = { actions.onChooseGoal(null) },
            )
            Rule()
        }
    }
}

@Composable
private fun GeneralGoalRow(title: String, tag: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) LevitxColors.Accent else LevitxColors.Text,
            modifier = Modifier.weight(1f),
        )
        Caption(tag, color = if (selected) LevitxColors.Accent else LevitxColors.TextMuted)
    }
}

/** All-round or focused, whether they can do a pull-up, and bodyweight if they want to give it. */
@Composable
internal fun HowStep(state: OnboardingUiState, actions: OnboardingActions) {
    StepTitle(stringResource(R.string.how_title))
    if (state.goalPathId != null) {
        Question(stringResource(R.string.how_plan_label)) {
            StyleOption(
                title = stringResource(R.string.how_all_round),
                badge = stringResource(R.string.how_suggested),
                body = stringResource(R.string.how_all_round_body),
                selected = state.style == PlanStyle.ALL_ROUND,
                onClick = { actions.onChooseStyle(PlanStyle.ALL_ROUND) },
            )
            StyleOption(
                title = stringResource(R.string.how_focused),
                badge = null,
                body = stringResource(R.string.how_focused_body),
                selected = state.style == PlanStyle.FOCUSED,
                onClick = { actions.onChooseStyle(PlanStyle.FOCUSED) },
            )
        }
    }
    Question(stringResource(R.string.how_pull_up_label), help = stringResource(R.string.how_pull_up_note)) {
        ChoiceRow {
            listOf(
                PullUpAnswer.YES to R.string.how_pull_up_yes,
                PullUpAnswer.NOT_YET to R.string.how_pull_up_not_yet,
                PullUpAnswer.NOT_SURE to R.string.how_pull_up_not_sure,
            ).forEach { (answer, label) ->
                ChoiceBar(stringResource(label), state.pullUp == answer, { actions.onAnswerPullUp(answer) })
            }
        }
    }
    Question(
        text = stringResource(R.string.how_bodyweight_label),
        trailing = stringResource(R.string.how_optional),
        help = stringResource(
            if (state.bodyweightInvalid) R.string.how_bodyweight_invalid else R.string.how_bodyweight_note,
        ),
    ) {
        KgField(state.bodyweightText, actions.onTypeBodyweight, invalid = state.bodyweightInvalid)
    }
}

/** A plan style, as a dark card with a round radio, as on the "How should we build it?" artboard. */
@Composable
private fun StyleOption(title: String, badge: String?, body: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .then(if (selected) Modifier.border(2.dp, LevitxColors.Accent) else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp)
                .border(
                    if (selected) 5.dp else 2.dp,
                    if (selected) LevitxColors.Accent else LevitxColors.Outline,
                    CircleShape,
                ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LevitxColors.Text)
                if (badge != null) {
                    Text(
                        text = badge,
                        style = LevitxType.Micro,
                        fontWeight = FontWeight.Medium,
                        color = LevitxColors.Accent,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Text(body, fontSize = 13.sp, lineHeight = 18.sp, color = LevitxColors.TextMuted)
        }
    }
}

/** A kilogram field with "KG" in its own cell on the right, as on the artboard. */
@Composable
private fun KgField(text: String, onChange: (String) -> Unit, invalid: Boolean) {
    val description = stringResource(R.string.how_bodyweight_description)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(LevitxColors.Surface)
            .border(1.dp, if (invalid) LevitxColors.Accent else LevitxColors.Tick),
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
                .align(Alignment.CenterVertically),
        ) {
            if (text.isEmpty()) {
                Text(
                    stringResource(R.string.how_bodyweight_hint),
                    style = LevitxType.Figure,
                    fontSize = 20.sp,
                    color = LevitxColors.TextFaint,
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onChange,
                singleLine = true,
                textStyle = LevitxType.Figure.copy(fontSize = 20.sp, color = LevitxColors.Text),
                cursorBrush = SolidColor(LevitxColors.Accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = description },
            )
        }
        Box(
            Modifier
                .width(1.dp)
                .height(52.dp)
                .background(LevitxColors.Tick),
        )
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp).height(52.dp)) {
            Caption(stringResource(R.string.kg))
        }
    }
}

/** Training days, time per session and kit. */
@Composable
internal fun WeekStep(state: OnboardingUiState, actions: OnboardingActions) {
    StepTitle(stringResource(R.string.week_title), stringResource(R.string.week_body))
    Question(
        text = stringResource(R.string.week_days_label),
        trailing = pluralStringResource(R.plurals.week_days_count, state.days.size, state.days.size),
    ) {
        // Read through the configuration so the day letters follow a language change.
        val locale = LocalConfiguration.current.locales[0]
        ChoiceRow {
            DayOfWeek.entries.forEach { day ->
                ChoiceBar(
                    text = day.getDisplayName(TextStyle.NARROW, locale),
                    selected = day in state.days,
                    onClick = { actions.onToggleDay(day) },
                    style = ChoiceStyle.CREAM,
                    multiple = true,
                    mono = true,
                    description = day.getDisplayName(TextStyle.FULL, locale),
                )
            }
        }
    }
    Question(stringResource(R.string.week_minutes_label)) {
        ChoiceRow {
            SessionLengths.forEach { minutes ->
                ChoiceBar(stringResource(R.string.minutes_short, minutes), state.minutes == minutes, {
                    actions.onChooseMinutes(minutes)
                })
            }
        }
    }
    KitQuestion(state, actions)
}

/** "What do you have?": nothing, or any of the kit, and what the goal still needs. */
@Composable
private fun KitQuestion(state: OnboardingUiState, actions: OnboardingActions) {
    Question(stringResource(R.string.week_kit_label), help = stringResource(R.string.week_kit_note)) {
        ChoiceRow {
            ChoiceBar(
                text = stringResource(R.string.kit_none),
                selected = state.equipment.isEmpty(),
                onClick = actions.onChooseNoEquipment,
            )
        }
        KitOrder.chunked(KIT_PER_ROW).forEach { row ->
            ChoiceRow {
                row.forEach { kit ->
                    ChoiceBar(
                        text = kitName(kit),
                        selected = kit in state.equipment,
                        onClick = { actions.onToggleEquipment(kit) },
                        multiple = true,
                    )
                }
            }
        }
    }
    val missing = state.missingGoalKit
    val path = state.goalPathId?.let(SkillCatalogue::byId)
    if (missing.isNotEmpty() && path != null) {
        SquareNote(
            title = null,
            text = stringResource(R.string.week_kit_missing, path.name, missing.joinToString { CoachPhrases.noun(it) }),
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** The plan in brief, with the first session the coach planned. */
@Composable
internal fun ReadyStep(state: OnboardingUiState) {
    val path = state.goalPathId?.let(SkillCatalogue::byId)
    val style = when {
        path == null -> R.string.ready_style_general
        state.style == PlanStyle.FOCUSED -> R.string.ready_style_focused
        else -> R.string.ready_style_all_round
    }
    Column(modifier = Modifier.padding(horizontal = ScreenPadding)) {
        Caption(stringResource(R.string.ready_label, stringResource(style)), color = LevitxColors.Accent)
        Text(
            text = path?.name ?: stringResource(R.string.ready_general),
            style = LevitxType.Poster.copy(fontSize = 56.sp, lineHeight = 52.sp),
            color = LevitxColors.Text,
            modifier = Modifier.padding(top = 10.dp),
        )
        Rule(Modifier.padding(top = 18.dp))
        StatStrip(
            listOfNotNull(
                path?.let { it.gates.size.toString() to stringResource(R.string.ready_gates) },
                stringResource(R.string.ready_times, state.days.size) to stringResource(R.string.ready_per_week),
                state.minutes.toString() to stringResource(R.string.ready_minutes),
            ),
        )
    }
    if (path != null) {
        Rule(Modifier.padding(top = 8.dp))
        GateTrack(gateCount = path.gates.size, firstGate = path.gates.first().title)
    }
    Rule()
    state.firstSession?.let { SessionList(it, bodyweightKg = state.bodyweightKg) }
    SquareNote(
        title = null,
        text = stringResource(R.string.ready_learning),
        filled = false,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/**
 * The gates to the skill as a row of squares, the first filled, and the
 * first gate named under it.
 */
@Composable
private fun GateTrack(gateCount: Int, firstGate: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 18.dp),
    ) {
        Row {
            Caption(
                pluralStringResource(R.plurals.ready_gates_to_skill, gateCount, gateCount),
                modifier = Modifier.weight(1f),
            )
            Caption(stringResource(R.string.ready_pass_to_move_on), color = LevitxColors.TextFaint)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            repeat(gateCount) { index ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .then(
                            if (index == 0) {
                                Modifier.background(LevitxColors.Accent)
                            } else {
                                Modifier.border(
                                    2.dp,
                                    if (index ==
                                        gateCount - 1
                                    ) {
                                        LevitxColors.Accent
                                    } else {
                                        LevitxColors.Outline
                                    },
                                )
                            },
                        ),
                )
                if (index < gateCount - 1) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(LevitxColors.Tick),
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.ready_first_gate, firstGate),
            fontSize = 13.sp,
            color = LevitxColors.Text,
        )
    }
}
