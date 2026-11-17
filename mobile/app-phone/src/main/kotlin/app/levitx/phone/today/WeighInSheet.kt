package app.levitx.phone.today

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.load.LoadLimits
import app.levitx.core.load.WeightUnit
import app.levitx.core.load.formatKg
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.LocalWeightUnit
import app.levitx.phone.ui.OutlineBar
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.parseKg
import app.levitx.phone.ui.shortDate
import app.levitx.phone.ui.unitCaps
import app.levitx.phone.ui.unitLabel
import app.levitx.phone.ui.weightWithUnit
import kotlin.math.roundToLong

private const val SCRIM_ALPHA = 0.72f
private const val TENTHS = 10.0
private const val MAX_CHARS = 6

/** Where the stepper starts when there's no reading to prefill, in kilograms. */
private const val NO_READING_START_KG = 70.0

private val StepperHeight = 72.dp
private val StepButtonWidth = 56.dp

/**
 * The quick weigh-in before a session, laid out as the "Coach: weekly
 * weigh-in" artboard: prefilled with the last reading, a step of 0.1 in the
 * person's unit, Save and start, or Skip this week. Either answer counts as
 * asked, so it won't come back for a week. Back or a tap outside closes it
 * without starting.
 *
 * @param ask What to prefill.
 * @param onSave Called with the weight in kilograms, always within [LoadLimits].
 * @param onSkip Starts without a weigh-in.
 * @param onDismiss Closes the sheet and doesn't start.
 */
@Composable
fun BoxScope.WeighInSheet(ask: WeighInAsk, onSave: (Double) -> Unit, onSkip: () -> Unit, onDismiss: () -> Unit) {
    val unit = LocalWeightUnit.current
    var text by rememberSaveable(unit) { mutableStateOf(ask.lastKg?.let { formatKg(unit.fromKg(it)) }.orEmpty()) }
    val kg = parseKg(text)?.let(unit::toKg)?.takeIf { it in LoadLimits.MIN_BODYWEIGHT_KG..LoadLimits.MAX_BODYWEIGHT_KG }
    val title = stringResource(if (ask.lastKg == null) R.string.weighin_title_first else R.string.weighin_title)
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevitxColors.Background.copy(alpha = SCRIM_ALPHA))
            .clickable(interactionSource = null, indication = null, onClick = onDismiss),
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .navigationBarsPadding()
            .padding(start = ScreenPadding, end = ScreenPadding, top = 20.dp, bottom = 20.dp)
            .semantics {
                paneTitle = title
                isTraversalGroup = true
            },
    ) {
        Column {
            Caption(stringResource(R.string.weighin_label), color = LevitxColors.Accent)
            Text(
                text = title,
                style = LevitxType.Poster.copy(fontSize = 30.sp, lineHeight = 30.sp),
                color = LevitxColors.Text,
                modifier = Modifier.padding(top = 8.dp).semantics { heading() },
            )
            Text(
                text = stringResource(R.string.weighin_body),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = LevitxColors.TextMuted,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Stepper(text, unit, onText = { text = it })
        Caption(note(ask, kg, text), color = LevitxColors.TextFaint)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AccentBar(
                text = stringResource(R.string.weighin_save),
                onClick = { kg?.let(onSave) },
                enabled = kg != null,
            )
            OutlineBar(text = stringResource(R.string.weighin_skip), onClick = onSkip)
        }
        Text(
            stringResource(R.string.weighin_private),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = LevitxColors.TextMuted,
        )
    }
}

/** − then the number in big type with its unit, then +, each step 0.1 in the person's unit. */
@Composable
private fun Stepper(text: String, unit: WeightUnit, onText: (String) -> Unit) {
    val label = stringResource(R.string.weighin_field, unitLabel(unit))
    val shown = parseKg(text) ?: unit.fromKg(NO_READING_START_KG)
    val step = { by: Int -> onText(formatKg(((shown * TENTHS).roundToLong() + by) / TENTHS)) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(StepperHeight)
            .background(LevitxColors.Background)
            .border(1.dp, LevitxColors.Tick),
    ) {
        StepButton("−", stringResource(R.string.weighin_less, unitLabel(unit)), onClick = { step(-1) })
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f),
        ) {
            BasicTextField(
                value = text,
                onValueChange = { onText(it.take(MAX_CHARS)) },
                singleLine = true,
                textStyle = LevitxType.Poster.copy(
                    fontSize = 40.sp,
                    color = LevitxColors.Text,
                    textAlign = TextAlign.End,
                ),
                cursorBrush = SolidColor(LevitxColors.Accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(120.dp).semantics { contentDescription = label },
            )
            Caption(unitCaps(), modifier = Modifier.padding(start = 8.dp))
        }
        StepButton("+", stringResource(R.string.weighin_more, unitLabel(unit)), onClick = { step(1) })
    }
}

/** A square − or + taking the stepper's full height. */
@Composable
private fun StepButton(symbol: String, label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(StepButtonWidth)
            .fillMaxHeight()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    ) { Text(symbol, fontSize = 22.sp, color = LevitxColors.Text) }
}

/** The last reading, or the allowed range when what's typed is outside it. */
@Composable
private fun note(ask: WeighInAsk, kg: Double?, text: String): String = when {
    kg == null && text.isNotBlank() -> stringResource(
        R.string.weighin_range,
        weightWithUnit(LoadLimits.MIN_BODYWEIGHT_KG),
        weightWithUnit(LoadLimits.MAX_BODYWEIGHT_KG),
    )

    ask.lastKg != null && ask.lastAtEpochMillis != null ->
        stringResource(R.string.weighin_last, weightWithUnit(ask.lastKg), shortDate(ask.lastAtEpochMillis))

    else -> ""
}
