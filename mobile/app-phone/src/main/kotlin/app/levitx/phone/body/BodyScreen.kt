package app.levitx.phone.body

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.WeighIn
import app.levitx.core.load.LoadLimits
import app.levitx.data.equipment.Band
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.DecimalInput
import app.levitx.phone.ui.Help
import app.levitx.phone.ui.KitPicker
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.QuietBar
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.TabHeader

/** The bottom bars, as on the artboard. */
internal val BodyBarHeight = 64.dp

/** Where the weigh-in field starts before anything has been entered. */
private const val DEFAULT_BODYWEIGHT_KG = 70.0

/**
 * Everything the person can do on the Body tab.
 *
 * @property onSaveBodyweight Saves a weigh-in, in kg.
 * @property onChooseEquipment Null clears the kit; a piece is added or taken away.
 * @property onAddBand Adds a band: its name and the kg of help it gives.
 * @property onDeleteBand Removes a band, by id.
 */
data class BodyActions(
    val onSaveBodyweight: (Double) -> Unit,
    val onChooseEquipment: (Equipment?) -> Unit,
    val onAddBand: (String, Double) -> Unit,
    val onDeleteBand: (String) -> Unit,
)

/** Body tab: connects [BodyViewModel] to [BodyScreen]. */
@Composable
fun BodyRoute(viewModel: BodyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BodyScreen(
        uiState = uiState,
        actions = BodyActions(
            onSaveBodyweight = viewModel::saveBodyweight,
            onChooseEquipment = viewModel::chooseEquipment,
            onAddBand = viewModel::addBand,
            onDeleteBand = viewModel::deleteBand,
        ),
    )
}

/**
 * The Body tab, laid out as the "Body and load" artboard: bodyweight as one
 * big number with its trend, what that means on the bar for each exercise
 * trained now, then the kit the plan can use and the bands. Weigh in is
 * pinned to the bottom.
 */
@Composable
fun BodyScreen(uiState: BodyUiState, actions: BodyActions, modifier: Modifier = Modifier) {
    var weighing by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize().background(LevitxColors.Background)) {
        TabHeader(stringResource(R.string.tab_body))
        when (uiState) {
            BodyUiState.Loading -> Unit

            BodyUiState.Unavailable -> Lede(
                text = stringResource(R.string.data_unavailable),
                modifier = Modifier.padding(ScreenPadding),
            )

            is BodyUiState.Ready -> {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    BodyweightTrend(uiState)
                    OnTheBar(uiState)
                    Kit(uiState.equipment, actions.onChooseEquipment)
                    Bands(
                        bands = uiState.bands,
                        bodyweightKg = uiState.trend?.currentKg,
                        onAddBand = actions.onAddBand,
                        onDeleteBand = actions.onDeleteBand,
                    )
                }
                if (weighing) {
                    WeighInBar(
                        startKg = uiState.trend?.currentKg ?: DEFAULT_BODYWEIGHT_KG,
                        onSave = {
                            actions.onSaveBodyweight(it)
                            weighing = false
                        },
                        onCancel = { weighing = false },
                    )
                } else {
                    AccentBar(
                        text = stringResource(R.string.body_weigh_in),
                        onClick = { weighing = true },
                        height = BodyBarHeight,
                    )
                }
            }
        }
    }
}

/** "YOUR KIT": what the plan may use, edited in place. */
@Composable
private fun Kit(equipment: Set<Equipment>, onChoose: (Equipment?) -> Unit) {
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp)) {
        Caption(stringResource(R.string.body_kit_label))
        Help(stringResource(R.string.body_kit_note), modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
        KitPicker(equipment, onChooseNone = { onChoose(null) }, onToggle = onChoose)
    }
}

/** The weigh-in field above Save and Cancel, in place of the Weigh in bar. */
@Composable
private fun WeighInBar(startKg: Double, onSave: (Double) -> Unit, onCancel: () -> Unit) {
    var entry by rememberSaveable { mutableDoubleStateOf(startKg) }
    val valid = entry in LoadLimits.MIN_BODYWEIGHT_KG..LoadLimits.MAX_BODYWEIGHT_KG
    Rule()
    DecimalInput(
        value = entry,
        onValueChange = { entry = it },
        label = stringResource(R.string.body_weigh_in_label),
        max = LoadLimits.MAX_BODYWEIGHT_KG,
        modifier = Modifier.padding(ScreenPadding),
    )
    Row(modifier = Modifier.height(BodyBarHeight)) {
        AccentBar(
            text = stringResource(R.string.body_weigh_in_save),
            onClick = { onSave(entry) },
            enabled = valid,
            height = BodyBarHeight,
            modifier = Modifier.weight(2f),
        )
        QuietBar(
            text = stringResource(R.string.body_weigh_in_cancel),
            onClick = onCancel,
            modifier = Modifier.weight(1f).height(BodyBarHeight),
        )
    }
}

@Preview
@Composable
private fun BodyScreenPreview() {
    val day = 24L * 60 * 60 * 1_000
    val weighIns = listOf(76.4, 76.0, 75.9, 75.5, 75.6, 75.1, 74.9).mapIndexed { i, kg -> WeighIn(i * 5 * day, kg) }
    val model = AthleteModel.starting(
        CoachProfile("first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR)),
        PullUpAnswer.YES,
    ).copy(weighIns = weighIns)
    LevitxTheme {
        BodyScreen(
            uiState = bodyState(model, listOf(Band("1", "Red", 22.5)), nowEpochMillis = 31 * day),
            actions = BodyActions({}, {}, { _, _ -> }, {}),
        )
    }
}
