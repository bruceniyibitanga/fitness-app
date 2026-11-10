package app.levitx.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R

/*
 * Small building blocks the phone screens share, so every screen has the same
 * buttons, labels and number entry.
 */

/** Minimum touch target height for the main action on a screen. */
private val PrimaryButtonHeight = 56.dp
private val ProgressBarHeight = 4.dp
private val StepperButtonWidth = 64.dp
private val NumberFieldWidth = 120.dp

/** Mono uppercase label above a value, like "TODAY". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = LevitxColors.TextMuted) {
    Text(text = text, style = LevitxType.Label, color = color, modifier = modifier)
}

/** The one main action on a screen: full width, accent, square. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PrimaryButtonHeight),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

/** A secondary action next to a [PrimaryButton]. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PrimaryButtonHeight),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * A flat, square-edged progress bar. Screen readers announce it as a
 * progress bar with its percentage.
 */
@Composable
fun FlatProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ProgressBarHeight)
            .background(LevitxColors.Track)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .background(LevitxColors.Accent),
        )
    }
}

/**
 * Whole-number entry with − and + buttons for quick changes and a field for
 * typing. Never goes below zero or above [max].
 */
@Composable
fun NumberInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    max: Int = MAX_ENTRY,
) {
    Column(modifier = modifier) {
        SectionLabel(text = label)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = LevitxSpacing.Small),
        ) {
            OutlinedButton(
                onClick = { onValueChange((value - 1).coerceAtLeast(0)) },
                modifier = Modifier
                    .width(StepperButtonWidth)
                    .heightIn(min = PrimaryButtonHeight),
            ) { Text(stringResource(R.string.decrease), style = MaterialTheme.typography.titleLarge) }
            OutlinedTextField(
                value = value.toString(),
                onValueChange = { text ->
                    // Ignore anything that isn't a whole number; empty means 0.
                    val digits = text.filter(Char::isDigit).take(MAX_DIGITS)
                    onValueChange((digits.toIntOrNull() ?: 0).coerceAtMost(max))
                },
                textStyle = LevitxType.Numeral.copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .padding(horizontal = LevitxSpacing.Small)
                    .width(NumberFieldWidth),
            )
            OutlinedButton(
                onClick = { onValueChange((value + 1).coerceAtMost(max)) },
                modifier = Modifier
                    .width(StepperButtonWidth)
                    .heightIn(min = PrimaryButtonHeight),
            ) { Text(stringResource(R.string.increase), style = MaterialTheme.typography.titleLarge) }
        }
    }
}

/**
 * Kilogram entry that accepts decimals, with − and + buttons that step by
 * [step]. Reports only valid numbers between 0 and [max]; while the field
 * holds something unfinished (like "7." or ""), the last valid value stands.
 */
@Composable
fun DecimalInput(
    value: Double,
    onValueChange: (Double) -> Unit,
    label: String,
    max: Double,
    modifier: Modifier = Modifier,
    step: Double = DEFAULT_KG_STEP,
) {
    var text by rememberSaveable { mutableStateOf(formatKg(value)) }
    // Follow changes made elsewhere (the buttons, or a new default) without
    // fighting the person while they type.
    LaunchedEffect(value) {
        if (parseKg(text) != value) text = formatKg(value)
    }
    Column(modifier = modifier) {
        SectionLabel(text = label)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = LevitxSpacing.Small),
        ) {
            OutlinedButton(
                onClick = { onValueChange((value - step).coerceIn(0.0, max)) },
                modifier = Modifier
                    .width(StepperButtonWidth)
                    .heightIn(min = PrimaryButtonHeight),
            ) { Text(stringResource(R.string.decrease), style = MaterialTheme.typography.titleLarge) }
            OutlinedTextField(
                value = text,
                onValueChange = { typed ->
                    text = typed.take(MAX_KG_CHARS)
                    parseKg(text)?.takeIf { it <= max }?.let(onValueChange)
                },
                textStyle = LevitxType.Numeral.copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .padding(horizontal = LevitxSpacing.Small)
                    .width(NumberFieldWidth),
            )
            OutlinedButton(
                onClick = { onValueChange((value + step).coerceIn(0.0, max)) },
                modifier = Modifier
                    .width(StepperButtonWidth)
                    .heightIn(min = PrimaryButtonHeight),
            ) { Text(stringResource(R.string.increase), style = MaterialTheme.typography.titleLarge) }
        }
    }
}

/** "72.5" or "70": one decimal place at most, and none when it's a whole number. */
fun formatKg(kg: Double): String {
    val tenths = Math.round(kg * TENTHS_PER_UNIT)
    return if (tenths % TENTHS_PER_UNIT == 0L) {
        (tenths / TENTHS_PER_UNIT).toString()
    } else {
        "${tenths / TENTHS_PER_UNIT}.${tenths % TENTHS_PER_UNIT}"
    }
}

/** Reads "72.5" or "72,5" (comma decimal locales). Null for anything that isn't a non-negative number. */
fun parseKg(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
    ?.takeIf { it.isFinite() && it >= 0.0 }

/** "8 reps" or "30 s", depending on what the gate measures. */
@Composable
fun measureText(measure: GateMeasure, value: Int): String = when (measure) {
    GateMeasure.REPS -> pluralStringResource(R.plurals.reps, value, value)
    GateMeasure.HOLD_SECONDS -> stringResource(R.string.seconds_short, value)
}

/** "2 min" or "90 s". */
@Composable
fun durationText(seconds: Int): String = if (seconds % SECONDS_PER_MINUTE == 0) {
    stringResource(R.string.minutes_short, seconds / SECONDS_PER_MINUTE)
} else {
    stringResource(R.string.seconds_short, seconds)
}

/** Largest number the entry accepts. No one does 999 reps in a set. */
private const val MAX_ENTRY = 999
private const val MAX_DIGITS = 3
private const val SECONDS_PER_MINUTE = 60
private const val DEFAULT_KG_STEP = 0.5
private const val MAX_KG_CHARS = 6
private const val TENTHS_PER_UNIT = 10L
