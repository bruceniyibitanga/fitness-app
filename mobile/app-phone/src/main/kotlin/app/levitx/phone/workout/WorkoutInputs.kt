package app.levitx.phone.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.load.AddedWeight
import app.levitx.core.load.BandAssistance
import app.levitx.core.load.LoadLimits
import app.levitx.core.load.LoadModifier
import app.levitx.core.load.calculateEffectiveLoad
import app.levitx.core.load.formatKg
import app.levitx.data.equipment.Band
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.ChoiceBar
import app.levitx.phone.ui.ChoiceRow
import app.levitx.phone.ui.DecimalInput
import app.levitx.phone.ui.Help
import app.levitx.phone.ui.MAX_REPS_LEFT
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.repsLeftLabel

/** Largest number the counter goes to. No one does 999 reps in a set. */
private const val MAX_ENTRY = 999

/**
 * The set as one huge number over its target, like "07/8", with square
 * minus and plus buttons to adjust it.
 */
@Composable
internal fun Counter(value: Int, target: Int, unit: String, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 18.dp)) {
        Text(
            text = value.toString().padStart(2, '0'),
            style = LevitxType.Hero.copy(fontSize = 168.sp, lineHeight = 136.sp),
            color = LevitxColors.Text,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            text = stringResource(R.string.workout_of_target, target),
            style = LevitxType.Poster,
            color = LevitxColors.TextFaint,
            modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp)) {
        Caption(unit, modifier = Modifier.weight(1f))
        ChoiceRow(modifier = Modifier.width(132.dp)) {
            ChoiceBar(stringResource(R.string.decrease), selected = false, onClick = {
                onChange((value - 1).coerceAtLeast(0))
            })
            ChoiceBar(stringResource(R.string.increase), selected = false, onClick = {
                onChange((value + 1).coerceAtMost(MAX_ENTRY))
            })
        }
    }
}

/** "How many more could you do?" as five cells, 0 to 4+. */
@Composable
internal fun RepsLeft(selected: Int, onSelect: (Int) -> Unit) {
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 20.dp)) {
        Text(
            text = stringResource(R.string.workout_reps_left_question),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = LevitxColors.Text,
            modifier = Modifier.semantics { heading() },
        )
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .border(1.dp, LevitxColors.Line)
                .selectableGroup(),
        ) {
            for (left in 0..MAX_REPS_LEFT) {
                val isSelected = left == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .background(if (isSelected) LevitxColors.Text else Color.Transparent)
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(left) }),
                ) {
                    Text(
                        text = repsLeftLabel(left),
                        style = LevitxType.Figure,
                        color = if (isSelected) LevitxColors.Background else LevitxColors.Text,
                    )
                }
            }
        }
        Help(stringResource(R.string.workout_reps_left_help), modifier = Modifier.padding(top = 10.dp))
    }
}

/** The load per rep for the set as chosen, or null without bodyweight or a load to track. */
internal fun loadPerRep(state: WorkoutUiState.Active, addedKg: Double, assistanceKg: Double): Double? {
    val profile = state.plan.exercise.loadProfile
    val bodyweight = state.bodyweightKg
    if (profile == null || bodyweight == null) return null
    val modifiers = buildList<LoadModifier> {
        if (addedKg > 0.0) add(AddedWeight(addedKg))
        if (assistanceKg > 0.0) add(BandAssistance(assistanceKg))
    }
    return calculateEffectiveLoad(bodyweight, profile, modifiers).totalKg
}

/**
 * Vest or belt weight, and which band (if any). The load per rep they add up
 * to shows in the line above the counter, so a change shows straight away.
 */
@Composable
internal fun LoadChooser(
    state: WorkoutUiState.Active,
    addedKg: Double,
    onAddedKgChange: (Double) -> Unit,
    bandId: String?,
    onBandChange: (String?) -> Unit,
    assistanceKg: Double,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 20.dp, bottom = 20.dp),
    ) {
        DecimalInput(
            value = addedKg,
            onValueChange = onAddedKgChange,
            label = stringResource(R.string.workout_added_weight_label),
            max = LoadLimits.MAX_MODIFIER_KG,
        )
        if (state.bands.isNotEmpty()) {
            Caption(stringResource(R.string.workout_band_label))
            (listOf<Band?>(null) + state.bands).chunked(2).forEach { pair ->
                ChoiceRow {
                    pair.forEach { band ->
                        ChoiceBar(
                            text = band?.let {
                                stringResource(R.string.workout_band_choice, it.name, formatKg(it.assistanceKg))
                            } ?: stringResource(R.string.workout_band_none),
                            selected = bandId == band?.id,
                            onClick = { onBandChange(band?.id) },
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (assistanceKg > 0.0 && state.plan.gateId != null) Help(stringResource(R.string.workout_band_note))
    }
}
