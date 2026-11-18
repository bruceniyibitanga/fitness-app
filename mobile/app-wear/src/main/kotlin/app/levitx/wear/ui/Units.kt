package app.levitx.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import app.levitx.core.load.WeightUnit
import app.levitx.core.load.formatKg
import app.levitx.wear.R

/*
 * Weights in the person's unit on the watch. The phone sends its kg or lb
 * choice with each sync; everything is still stored and worked out in
 * kilograms, and these only change what's shown.
 */

/** The unit weights are shown in, provided once by the activity. Kilograms until settings are read. */
val LocalWeightUnit = staticCompositionLocalOf { WeightUnit.KG }

/** "kg" or "lb". */
@Composable
fun unitLabel(unit: WeightUnit = LocalWeightUnit.current): String =
    stringResource(if (unit == WeightUnit.KG) R.string.unit_kg else R.string.unit_lb)

/** [kg] as a number in the person's unit, to one decimal at most: "5", "11". */
@Composable
fun weightNumber(kg: Double): String = formatKg(LocalWeightUnit.current.fromKg(kg))

/** One tap of plus or minus on the watch: 1 kg, or 2.5 lb (the smallest common plate). */
fun WeightUnit.stepKg(): Double = toKg(if (this == WeightUnit.KG) 1.0 else LB_STEP)

private const val LB_STEP = 2.5
