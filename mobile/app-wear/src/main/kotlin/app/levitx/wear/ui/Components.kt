package app.levitx.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.wear.R

/** "8 reps" or "30 s", depending on what the gate measures. */
@Composable
fun measureText(measure: GateMeasure, value: Int): String = when (measure) {
    GateMeasure.REPS -> pluralStringResource(R.plurals.reps, value, value)
    GateMeasure.HOLD_SECONDS -> stringResource(R.string.seconds_short, value)
}

/** A small capital label above a value, in the accent colour unless [color] says otherwise. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = LevitxColors.Accent) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        textAlign = TextAlign.Center,
    )
}

/** Centred body text that wraps across the round screen. */
@Composable
fun CenteredText(text: String, modifier: Modifier = Modifier, color: Color = LevitxColors.TextSoft) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodySmall,
        color = color,
        textAlign = TextAlign.Center,
    )
}

/**
 * A number with a minus button on its left and a plus button on its right.
 *
 * The buttons are full-size round targets, because they are tapped with a
 * tired hand straight after a set.
 *
 * @param valueText The number as shown.
 * @param onDecrease Called for one step down.
 * @param onIncrease Called for one step up.
 * @param decreaseDescription What the minus button does, for screen readers.
 * @param increaseDescription What the plus button does, for screen readers.
 * @param canDecrease False at the lowest value.
 * @param canIncrease False at the highest value.
 */
@Composable
fun StepperRow(
    valueText: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
    canDecrease: Boolean = true,
    canIncrease: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onDecrease,
            modifier = Modifier.semantics { contentDescription = decreaseDescription },
            enabled = canDecrease,
            colors = TextButtonDefaults.filledTonalTextButtonColors(),
        ) { Text(stringResource(R.string.decrease)) }
        Text(
            text = valueText,
            style = MaterialTheme.typography.numeralMedium,
            // Screen readers hear the new value after each tap without it stealing focus.
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        TextButton(
            onClick = onIncrease,
            modifier = Modifier.semantics { contentDescription = increaseDescription },
            enabled = canIncrease,
            colors = TextButtonDefaults.filledTonalTextButtonColors(),
        ) { Text(stringResource(R.string.increase)) }
    }
}
