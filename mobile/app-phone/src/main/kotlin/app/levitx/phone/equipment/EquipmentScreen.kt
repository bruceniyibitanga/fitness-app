package app.levitx.phone.equipment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.load.LoadLimits
import app.levitx.core.load.bandAssistanceFromScale
import app.levitx.core.load.formatKg
import app.levitx.data.equipment.Band
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.DecimalInput
import app.levitx.phone.ui.PrimaryButton
import app.levitx.phone.ui.SecondaryButton
import app.levitx.phone.ui.SectionLabel

/** Starting value for the bodyweight field before anything is entered. */
private const val DEFAULT_BODYWEIGHT_KG = 70.0

/** Longest band name the field accepts. Matches the data layer's limit. */
private const val MAX_BAND_NAME_LENGTH = 30

/** Equipment destination: connects [EquipmentViewModel] to [EquipmentScreen]. */
@Composable
fun EquipmentRoute(onBack: () -> Unit, viewModel: EquipmentViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    EquipmentScreen(
        uiState = uiState,
        onBack = onBack,
        onSaveBodyweight = viewModel::saveBodyweight,
        onAddBand = viewModel::addBand,
        onDeleteBand = viewModel::deleteBand,
    )
}

/** Bodyweight, then bands with an add-and-calibrate form. */
@Composable
fun EquipmentScreen(
    uiState: EquipmentUiState,
    onBack: () -> Unit,
    onSaveBodyweight: (Double) -> Unit,
    onAddBand: (name: String, assistanceKg: Double) -> Unit,
    onDeleteBand: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Medium),
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LevitxSpacing.Medium, vertical = LevitxSpacing.Medium),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            when (uiState) {
                EquipmentUiState.Loading -> Unit

                EquipmentUiState.Unavailable -> Text(stringResource(R.string.data_unavailable))

                is EquipmentUiState.Ready -> {
                    SectionLabel(stringResource(R.string.equipment_label))
                    Text(stringResource(R.string.equipment_title), style = LevitxType.Display)
                    Text(
                        text = stringResource(R.string.equipment_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = LevitxColors.TextSoft,
                    )
                    BodyweightSection(uiState.bodyweightKg, onSaveBodyweight)
                    HorizontalDivider(color = LevitxColors.Line)
                    BandsSection(uiState.bands, uiState.bodyweightKg, onAddBand, onDeleteBand)
                }
            }
        }
    }
}

@Composable
private fun BodyweightSection(bodyweightKg: Double?, onSave: (Double) -> Unit) {
    var entry by rememberSaveable(bodyweightKg) { mutableDoubleStateOf(bodyweightKg ?: DEFAULT_BODYWEIGHT_KG) }
    val valid = entry in LoadLimits.MIN_BODYWEIGHT_KG..LoadLimits.MAX_BODYWEIGHT_KG
    DecimalInput(
        value = entry,
        onValueChange = { entry = it },
        label = stringResource(R.string.equipment_bodyweight_label),
        max = LoadLimits.MAX_BODYWEIGHT_KG,
    )
    if (bodyweightKg != null) {
        Text(
            text = stringResource(R.string.equipment_bodyweight_current, formatKg(bodyweightKg)),
            style = MaterialTheme.typography.bodyMedium,
            color = LevitxColors.TextMuted,
        )
    }
    PrimaryButton(
        text = stringResource(R.string.equipment_bodyweight_save),
        onClick = { onSave(entry) },
        enabled = valid && entry != bodyweightKg,
    )
}

@Composable
private fun BandsSection(
    bands: List<Band>,
    bodyweightKg: Double?,
    onAddBand: (String, Double) -> Unit,
    onDeleteBand: (String) -> Unit,
) {
    SectionLabel(stringResource(R.string.equipment_bands_label))
    if (bands.isEmpty()) {
        Text(
            text = stringResource(R.string.equipment_no_bands),
            style = MaterialTheme.typography.bodyMedium,
            color = LevitxColors.TextMuted,
        )
    }
    bands.forEach { band ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(band.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.equipment_band_help, formatKg(band.assistanceKg)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LevitxColors.TextMuted,
                )
            }
            TextButton(onClick = { onDeleteBand(band.id) }) { Text(stringResource(R.string.equipment_band_remove)) }
        }
    }
    if (bodyweightKg == null) {
        Text(
            text = stringResource(R.string.equipment_band_needs_bodyweight),
            style = MaterialTheme.typography.bodyMedium,
            color = LevitxColors.TextMuted,
        )
    } else {
        AddBandForm(bodyweightKg, onAddBand)
    }
}

/**
 * Adds a band by calibrating it on a bathroom scale. The steps are shown in
 * full because the measurement is only as good as how it's done.
 */
@Composable
private fun AddBandForm(bodyweightKg: Double, onAddBand: (String, Double) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    if (!open) {
        SecondaryButton(stringResource(R.string.equipment_band_add), onClick = { open = true })
        return
    }
    var name by rememberSaveable { mutableStateOf("") }
    var reading by rememberSaveable { mutableDoubleStateOf(bodyweightKg) }
    val assistance = bandAssistanceFromScale(bodyweightKg, reading)

    Surface(
        color = LevitxColors.Surface,
        border = BorderStroke(Dp.Hairline, LevitxColors.Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Medium),
            modifier = Modifier.padding(LevitxSpacing.Medium),
        ) {
            SectionLabel(stringResource(R.string.equipment_calibrate_label))
            Text(
                text = stringResource(R.string.equipment_calibrate_steps),
                style = MaterialTheme.typography.bodyMedium,
                color = LevitxColors.TextSoft,
            )
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
                style = MaterialTheme.typography.titleMedium,
                color = if (assistance != null) LevitxColors.Accent else LevitxColors.TextMuted,
            )
            PrimaryButton(
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
}

@Preview
@Composable
private fun EquipmentScreenPreview() {
    LevitxTheme {
        EquipmentScreen(
            uiState = EquipmentUiState.Ready(74.5, listOf(Band("1", "Red", 22.5))),
            onBack = {},
            onSaveBodyweight = {},
            onAddBand = { _, _ -> },
            onDeleteBand = {},
        )
    }
}
