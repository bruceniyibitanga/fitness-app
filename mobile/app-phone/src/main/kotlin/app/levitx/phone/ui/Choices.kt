package app.levitx.phone.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.MovementPattern
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R

/*
 * Choice controls shared by setup, Today and the workout screen. Each is a
 * real selectable or toggleable element, so screen readers announce it as a
 * radio button or checkbox with its state.
 */

private val ChoiceHeight = 48.dp
private val RadioSize = 18.dp
private val RadioRingSelected = 5.dp
private val RadioRing = 2.dp
private val SelectedOutline = 2.dp

/**
 * One option of a single choice, with a title, a line of explanation and an
 * optional [badge] (like "SUGGESTED").
 */
@Composable
fun OptionCard(
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    trailing: String? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LevitxSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .background(LevitxColors.Surface)
            .then(if (selected) Modifier.border(BorderStroke(SelectedOutline, LevitxColors.Accent)) else Modifier)
            .padding(LevitxSpacing.Medium),
    ) {
        Box(
            modifier = Modifier
                .size(RadioSize)
                .border(
                    BorderStroke(
                        if (selected) RadioRingSelected else RadioRing,
                        if (selected) LevitxColors.Accent else LevitxColors.Tick,
                    ),
                    CircleShape,
                ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(LevitxSpacing.XSmall), modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(LevitxSpacing.Small)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                badge?.let { Text(it, style = LevitxType.Label, color = LevitxColors.Accent) }
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = LevitxColors.TextMuted)
        }
        trailing?.let { Text(it, style = LevitxType.Label, color = LevitxColors.TextMuted) }
    }
}

/** One button of a row of short single-choice options, like "20 min" or "Not yet". Use inside a [Row]. */
@Composable
fun RowScope.ChoiceButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = ChoiceHeight)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .background(if (selected) LevitxColors.Accent else LevitxColors.Surface)
            .then(if (selected) Modifier else Modifier.border(BorderStroke(1.dp, LevitxColors.Tick))),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) LevitxColors.Background else LevitxColors.Text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = LevitxSpacing.XSmall),
        )
    }
}

/** An on-or-off option, like a day of the week or a piece of kit. */
@Composable
fun ToggleChip(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = ChoiceHeight)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Checkbox)
            .background(if (checked) LevitxColors.Text else LevitxColors.Surface)
            .then(if (checked) Modifier else Modifier.border(BorderStroke(1.dp, LevitxColors.Tick)))
            .padding(horizontal = LevitxSpacing.Medium),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = if (checked) LevitxColors.Background else LevitxColors.Text,
        )
    }
}

/** What the apps call a movement pattern. */
@Composable
fun patternName(pattern: MovementPattern): String = stringResource(
    when (pattern) {
        MovementPattern.VERTICAL_PULL -> R.string.pattern_vertical_pull
        MovementPattern.HORIZONTAL_PULL -> R.string.pattern_horizontal_pull
        MovementPattern.VERTICAL_PUSH -> R.string.pattern_vertical_push
        MovementPattern.HORIZONTAL_PUSH -> R.string.pattern_horizontal_push
        MovementPattern.SQUAT -> R.string.pattern_squat
        MovementPattern.HINGE -> R.string.pattern_hinge
        MovementPattern.CORE -> R.string.pattern_core
    },
)

/** What the apps call a piece of kit. */
@Composable
fun kitName(equipment: Equipment): String = stringResource(
    when (equipment) {
        Equipment.PULL_UP_BAR -> R.string.kit_pull_up_bar
        Equipment.LOW_BAR -> R.string.kit_low_bar
        Equipment.DIP_BARS -> R.string.kit_dip_bars
        Equipment.RINGS -> R.string.kit_rings
        Equipment.BAND -> R.string.kit_band
    },
)
