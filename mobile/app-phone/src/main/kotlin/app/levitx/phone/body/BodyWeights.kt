package app.levitx.phone.body

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.load.LoadKit
import app.levitx.core.load.WeightUnit
import app.levitx.core.load.formatWeight
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.ChoiceBar
import app.levitx.phone.ui.ChoiceRow
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.weightText
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * Weights on the Body tab: a vest, a dip belt and plates. The coach only adds
 * weight once there is no harder version of an exercise for the person, and
 * only in steps the kit can actually make, so this is what it plans with.
 */

/** Plate sizes people own, lightest first, in the unit they buy them in. */
private val KG_PLATES = listOf(1.25, 2.5, 5.0, 10.0, 15.0, 20.0)
private val LB_PLATES = listOf(2.5, 5.0, 10.0, 25.0, 35.0, 45.0)

/** The steps an adjustable vest comes in. 0 is a vest of one fixed weight. */
private val KG_VEST_STEPS = listOf(0.0, 0.5, 1.0, 2.5)
private val LB_VEST_STEPS = listOf(0.0, 1.0, 2.0, 5.0)

/** The vest's weight moves by this much a tap: 1 kg or 2 lb. */
private fun vestTap(unit: WeightUnit): Double = if (unit == WeightUnit.KG) 1.0 else 2.0

/** Heaviest vest offered, in kilograms or pounds: more than any sold. */
private const val HEAVIEST_VEST_KG = 50.0
private const val HEAVIEST_VEST_LB = 110.0

private fun heaviestVest(unit: WeightUnit): Double = if (unit == WeightUnit.KG) HEAVIEST_VEST_KG else HEAVIEST_VEST_LB

/** Most plates of one size. */
private const val MAX_PER_SIZE = 6

/** Weights within this many kilograms are the same plate. */
private const val SAME_PLATE_KG = 0.005

/** "WEIGHTS": the vest, the belt and the plates, each edited in place. */
@Composable
internal fun Weights(kit: LoadKit, unit: WeightUnit, onChange: (LoadKit) -> Unit) {
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 28.dp)) {
        Caption(stringResource(R.string.body_weights_label), modifier = Modifier.padding(bottom = 8.dp))
        Vest(kit, unit, onChange)
        ChoiceRow(modifier = Modifier.padding(top = 12.dp)) {
            ChoiceBar(
                text = stringResource(R.string.body_belt),
                selected = kit.hasBelt,
                onClick = { onChange(kit.copy(hasBelt = !kit.hasBelt)) },
                multiple = true,
            )
        }
        Caption(stringResource(R.string.body_plates_label), modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
        plateSizes(kit, unit).forEach { sizeKg ->
            val count = kit.platesKg.count { abs(it - sizeKg) < SAME_PLATE_KG }
            CountRow(
                label = weightText(sizeKg, unit),
                value = count.toString(),
                onLess = { onChange(kit.copy(platesKg = kit.platesKg.withoutOne(sizeKg))) },
                onMore = {
                    if (count < MAX_PER_SIZE) onChange(kit.copy(platesKg = kit.platesKg + sizeKg))
                },
            )
        }
    }
}

/** The vest: how heavy it goes, then the steps it adjusts in. */
@Composable
private fun Vest(kit: LoadKit, unit: WeightUnit, onChange: (LoadKit) -> Unit) {
    val inUnit = unit.fromKg(kit.vestMaxKg)
    val tap = vestTap(unit)
    CountRow(
        label = stringResource(R.string.body_vest),
        value = if (kit.vestMaxKg == 0.0) stringResource(R.string.body_vest_none) else weightText(kit.vestMaxKg, unit),
        onLess = { onChange(kit.withVest(unit.toKg(((inUnit - tap) / tap).roundToInt() * tap))) },
        onMore = {
            val heavier = ((inUnit + tap) / tap).roundToInt() * tap
            if (heavier <= heaviestVest(unit)) onChange(kit.withVest(unit.toKg(heavier)))
        },
    )
    if (kit.vestMaxKg > 0.0) {
        Caption(stringResource(R.string.body_vest_steps), modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
        ChoiceRow {
            (if (unit == WeightUnit.KG) KG_VEST_STEPS else LB_VEST_STEPS).forEach { step ->
                val stepKg = unit.toKg(step)
                ChoiceBar(
                    text = if (step == 0.0) stringResource(R.string.body_vest_fixed) else formatWeight(stepKg, unit),
                    selected = abs(kit.vestStepKg - stepKg) < SAME_PLATE_KG,
                    onClick = { onChange(kit.copy(vestStepKg = stepKg.coerceAtMost(kit.vestMaxKg))) },
                    mono = true,
                )
            }
        }
    }
}

/** A label, then − value + as square buttons. */
@Composable
private fun CountRow(label: String, value: String, onLess: () -> Unit, onMore: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = LevitxColors.Text,
            modifier = Modifier.weight(1f),
        )
        Square(stringResource(R.string.decrease), stringResource(R.string.body_less, label), onLess)
        Text(
            text = value,
            style = LevitxType.Caption,
            color = LevitxColors.Text,
            modifier = Modifier
                .width(84.dp)
                .padding(horizontal = 6.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            textAlign = TextAlign.Center,
        )
        Square(stringResource(R.string.increase), stringResource(R.string.body_more, label), onMore)
    }
}

@Composable
private fun Square(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .border(1.dp, LevitxColors.Tick)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
    ) {
        Text(symbol, fontSize = 18.sp, color = LevitxColors.Text)
    }
}

/**
 * The plate sizes to list: the usual ones in the person's unit, plus any
 * they already own in another size, so a plate added in kilograms can still
 * be taken away after switching to pounds.
 */
private fun plateSizes(kit: LoadKit, unit: WeightUnit): List<Double> {
    val usual = (if (unit == WeightUnit.KG) KG_PLATES else LB_PLATES).map(unit::toKg)
    val others = kit.platesKg.filter { owned -> usual.none { abs(it - owned) < SAME_PLATE_KG } }
    return (usual + others).distinctBy { (it / SAME_PLATE_KG).roundToInt() }.sorted()
}

/** The kit with its vest going up to [maxKg], keeping the step only while it still fits. */
private fun LoadKit.withVest(maxKg: Double): LoadKit {
    val max = maxKg.coerceAtLeast(0.0)
    return copy(vestMaxKg = max, vestStepKg = if (vestStepKg <= max) vestStepKg else 0.0)
}

/** This list with one plate of [sizeKg] taken away, if there is one. */
private fun List<Double>.withoutOne(sizeKg: Double): List<Double> {
    val index = indexOfFirst { abs(it - sizeKg) < SAME_PLATE_KG }
    return if (index < 0) this else filterIndexed { i, _ -> i != index }
}
