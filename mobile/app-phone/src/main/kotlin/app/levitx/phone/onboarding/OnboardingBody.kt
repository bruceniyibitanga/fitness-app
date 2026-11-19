package app.levitx.phone.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.CoachPhrases
import app.levitx.core.load.LoadLimits
import app.levitx.core.load.WeightUnit
import app.levitx.core.load.formatKg
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.ChoiceBar
import app.levitx.phone.ui.ChoiceRow
import app.levitx.phone.ui.ChoiceStyle
import app.levitx.phone.ui.KitPicker
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.unitLabel
import java.time.DayOfWeek
import java.time.format.TextStyle

/*
 * The questions about the person and their week, each on its own screen as
 * its artboard in "Levitx App Design" (OnbBody, OnbSchedule, OnbKit).
 */

private val FieldHeight = 72.dp
private val UnitWidth = 56.dp

/**
 * Bodyweight, with the unit beside it. The unit is the whole app's: picking
 * one here is the same as picking it in Settings, and the number typed is
 * read in whichever is picked. Optional: the step can be skipped.
 */
@Composable
internal fun BodyStep(state: OnboardingUiState, actions: OnboardingActions) {
    StepTitle(stringResource(R.string.body_title))
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp)
            .height(FieldHeight),
    ) {
        WeightField(state, actions.onTypeBodyweight, modifier = Modifier.weight(1f))
        UnitSwitch(state.bodyweightUnit, actions.onChooseUnit)
    }
    if (state.bodyweightInvalid) {
        val unit = state.bodyweightUnit
        StepNote(
            text = stringResource(
                R.string.how_bodyweight_invalid,
                weightIn(unit, LoadLimits.MIN_BODYWEIGHT_KG),
                weightIn(unit, LoadLimits.MAX_BODYWEIGHT_KG),
            ),
            warning = true,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    } else {
        StepNote(stringResource(R.string.how_bodyweight_note))
    }
}

/** "20 kg" or "44 lb": [kg] in [unit], which is the unit on screen even before the setting catches up. */
@Composable
private fun weightIn(unit: WeightUnit, kg: Double): String =
    stringResource(R.string.weight_value, formatKg(unit.fromKg(kg)), unitLabel(unit))

/** The number field. Its outline turns orange while what is typed isn't a usable weight. */
@Composable
private fun WeightField(state: OnboardingUiState, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val unitName = stringResource(
        if (state.bodyweightUnit == WeightUnit.KG) R.string.unit_kg_name else R.string.unit_lb_name,
    )
    val description = stringResource(R.string.how_bodyweight_description, unitName)
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxHeight()
            .background(LevitxColors.Surface)
            .then(if (state.bodyweightInvalid) Modifier.border(1.dp, LevitxColors.Accent) else Modifier)
            .padding(horizontal = 16.dp),
    ) {
        if (state.bodyweightText.isEmpty()) {
            Text(
                text = stringResource(R.string.how_bodyweight_hint),
                style = LevitxType.Figure,
                fontSize = 28.sp,
                color = LevitxColors.TextFaint,
            )
        }
        BasicTextField(
            value = state.bodyweightText,
            onValueChange = onChange,
            singleLine = true,
            textStyle = LevitxType.Figure.copy(fontSize = 28.sp, color = LevitxColors.Text),
            cursorBrush = SolidColor(LevitxColors.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = description },
        )
    }
}

/** KG and LB side by side, the one in use filled. */
@Composable
private fun UnitSwitch(selected: WeightUnit, onChoose: (WeightUnit) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = modifier.selectableGroup()) {
        WeightUnit.entries.forEach { unit ->
            val on = unit == selected
            val name = stringResource(if (unit == WeightUnit.KG) R.string.unit_kg_name else R.string.unit_lb_name)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(UnitWidth)
                    .fillMaxHeight()
                    .background(if (on) LevitxColors.Text else LevitxColors.Surface)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onChoose(unit) })
                    .semantics { contentDescription = name },
            ) {
                Text(
                    text = unitLabel(unit).uppercase(),
                    style = LevitxType.Caption,
                    color = if (on) LevitxColors.Background else LevitxColors.Text,
                )
            }
        }
    }
}

/** Training days and time per session. */
@Composable
internal fun WeekStep(state: OnboardingUiState, actions: OnboardingActions) {
    StepTitle(stringResource(R.string.week_title))
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
                ChoiceBar(
                    text = stringResource(R.string.minutes_short, minutes),
                    selected = state.minutes == minutes,
                    onClick = { actions.onChooseMinutes(minutes) },
                )
            }
        }
    }
}

/** The kit they can train on: nothing, or any of it, and what the goal still needs. */
@Composable
internal fun KitStep(state: OnboardingUiState, actions: OnboardingActions) {
    StepTitle(stringResource(R.string.kit_title))
    KitPicker(
        equipment = state.equipment,
        onChooseNone = actions.onChooseNoEquipment,
        onToggle = actions.onToggleEquipment,
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp),
    )
    val missing = state.missingGoalKit
    val path = state.goalPathId?.let(SkillCatalogue::byId)
    if (missing.isNotEmpty() && path != null) {
        // The goal can't be trained on this kit, so say so before the plan quietly leaves it out.
        StepNote(
            text = stringResource(R.string.week_kit_missing, path.name, missing.joinToString { CoachPhrases.noun(it) }),
            warning = true,
        )
    } else {
        StepNote(stringResource(R.string.week_kit_note))
    }
}
