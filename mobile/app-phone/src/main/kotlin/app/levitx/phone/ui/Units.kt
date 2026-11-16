package app.levitx.phone.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import app.levitx.core.load.WeightUnit
import app.levitx.core.load.formatWeight
import app.levitx.phone.R

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

/** [kg] in the person's unit with its label: "12.5 kg" or "27.56 lb". */
@Composable
fun weightText(kg: Double, unit: WeightUnit = LocalWeightUnit.current): String =
    stringResource(R.string.weight_value, formatWeight(kg, unit), unitLabel(unit))
