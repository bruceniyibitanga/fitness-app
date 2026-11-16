package app.levitx.phone.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import app.levitx.core.load.WeightUnit
import app.levitx.core.load.formatKg
import app.levitx.core.load.formatWeight
import app.levitx.phone.R

/*
 * Weights in the person's unit. Everything is stored and worked out in
 * kilograms; these turn a figure into what the person reads. Kit weights
 * (plates, a vest) keep two decimals so a 1.25 kg plate reads as one. Loads and
 * bodyweight keep one, as the rest of the app always has.
 */

/**
 * The unit the person sees weights in. Provided once at the top of the app
 * from the settings, so every screen shows the same unit without each one
 * reading the settings itself. Kilograms until the settings are read.
 */
val LocalWeightUnit = staticCompositionLocalOf { WeightUnit.KG }

/** The short name of [unit]: "kg" or "lb". */
@Composable
fun unitLabel(unit: WeightUnit = LocalWeightUnit.current): String =
    stringResource(if (unit == WeightUnit.KG) R.string.unit_kg else R.string.unit_lb)

/** The short name of the person's unit in capitals, for captions: "KG" or "LB". */
@Composable
fun unitCaps(): String = unitLabel().uppercase()

/** A kit weight with its unit, to two decimals: "1.25 kg", "45 lb". */
@Composable
fun weightText(kg: Double, unit: WeightUnit = LocalWeightUnit.current): String =
    stringResource(R.string.weight_value, formatWeight(kg, unit), unitLabel(unit))

/** A load or bodyweight as a bare number, to one decimal, in the person's unit: "76.3". */
@Composable
fun weightNumber(kg: Double): String = formatKg(LocalWeightUnit.current.fromKg(kg))

/** A load or bodyweight with its unit, to one decimal: "76.3 kg". */
@Composable
fun weightWithUnit(kg: Double): String = stringResource(R.string.weight_value, weightNumber(kg), unitLabel())

/** A change in weight with its sign, in the person's unit: "+1.2", "−0.4", or "0". */
@Composable
fun signedWeight(kg: Double): String = signedKg(LocalWeightUnit.current.fromKg(kg))

/** "76.3 kg", the load of one rep. */
@Composable
fun perRepText(kg: Double): String = weightWithUnit(kg)

/** "KG PER REP", the caption for a load figure. */
@Composable
fun perRepCaption(): String = stringResource(R.string.figure_kg_per_rep, unitCaps())
