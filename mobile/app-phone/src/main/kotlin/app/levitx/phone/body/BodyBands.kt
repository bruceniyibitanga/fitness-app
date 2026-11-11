package app.levitx.phone.body

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.load.LoadLimits
import app.levitx.core.load.bandAssistanceFromScale
import app.levitx.core.load.formatKg
import app.levitx.data.equipment.Band
import app.levitx.data.equipment.MAX_BAND_NAME_LENGTH
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.DecimalInput
import app.levitx.phone.ui.Help
import app.levitx.phone.ui.OutlineBar
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding

/*
 * Bands on the Body tab: the ones calibrated so far, and calibrating a new
 * one on a bathroom scale. The help a band gives is what turns an assisted
 * set into kilograms per rep.
 */

/** "BANDS", each band with its help and Remove, then Add a band. */
@Composable
internal fun Bands(
    bands: List<Band>,
    bodyweightKg: Double?,
    onAddBand: (String, Double) -> Unit,
    onDeleteBand: (String) -> Unit,
) {
    Caption(
        stringResource(R.string.equipment_bands_label),
        modifier = Modifier.padding(start = ScreenPadding, top = 28.dp, bottom = 10.dp),
    )
    Rule()
    bands.forEach { band ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = ScreenPadding),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(band.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LevitxColors.Text)
                Text(
                    text = stringResource(R.string.equipment_band_help, formatKg(band.assistanceKg)),
                    style = LevitxType.Micro,
                    color = LevitxColors.TextFaint,
                )
            }
            Caption(
                text = stringResource(R.string.equipment_band_remove),
                modifier = Modifier
                    .clickable(role = Role.Button) { onDeleteBand(band.id) }
                    .padding(horizontal = ScreenPadding, vertical = 18.dp),
            )
        }
        Rule()
    }
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 14.dp, bottom = 28.dp)) {
        when {
            bodyweightKg == null -> Help(stringResource(R.string.equipment_band_needs_bodyweight))
            else -> AddBand(bodyweightKg, onAddBand)
        }
    }
}

/**
 * Adds a band by calibrating it on a bathroom scale. The steps are shown in
 * full because the measurement is only as good as how it's done.
 */
@Composable
private fun AddBand(bodyweightKg: Double, onAddBand: (String, Double) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    if (!open) {
        OutlineBar(stringResource(R.string.equipment_band_add), onClick = { open = true })
        return
    }
    var name by rememberSaveable { mutableStateOf("") }
    var reading by rememberSaveable { mutableDoubleStateOf(bodyweightKg) }
    val assistance = bandAssistanceFromScale(bodyweightKg, reading)
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .border(1.dp, LevitxColors.Line)
            .padding(16.dp),
    ) {
        Caption(stringResource(R.string.equipment_calibrate_label), color = LevitxColors.Accent)
        Help(stringResource(R.string.equipment_calibrate_steps))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(MAX_BAND_NAME_LENGTH) },
            label = { Text(stringResource(R.string.equipment_band_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        DecimalInput(
            value = reading,
            onValueChange = { reading = it },
            label = stringResource(R.string.equipment_scale_reading),
            max = LoadLimits.MAX_BODYWEIGHT_KG,
        )
        Text(
            text = assistance?.let { stringResource(R.string.equipment_band_result, formatKg(it)) }
                ?: stringResource(R.string.equipment_band_result_invalid),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (assistance != null) LevitxColors.Accent else LevitxColors.TextMuted,
        )
        AccentBar(
            text = stringResource(R.string.equipment_band_save),
            enabled = assistance != null && name.isNotBlank(),
            onClick = {
                if (assistance != null) {
                    onAddBand(name, assistance)
                    open = false
                    name = ""
                }
            },
        )
    }
}
