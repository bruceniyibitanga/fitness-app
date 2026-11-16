package app.levitx.phone.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.data.settings.LEAD_IN_CHOICES
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.BuildConfig
import app.levitx.phone.R
import app.levitx.phone.ui.BackHeader
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.ConfirmSheet
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding

/** The export's suggested file name. The person can change it in the save dialog. */
private const val EXPORT_FILE_NAME = "levitx-export.zip"

/**
 * What Settings can do.
 *
 * @property onLeadIn Sets the count-in, in seconds.
 * @property onSounds Turns beeps on or off.
 * @property onBuzzes Turns buzzes on or off.
 * @property onExport Writes the export to the chosen file's uri.
 * @property onDeleteEverything Deletes everything, once confirmed.
 */
data class SettingsActions(
    val onLeadIn: (Int) -> Unit,
    val onSounds: (Boolean) -> Unit,
    val onBuzzes: (Boolean) -> Unit,
    val onExport: (String) -> Unit,
    val onDeleteEverything: () -> Unit,
)

/**
 * Settings destination: connects [SettingsViewModel] to [SettingsScreen], and
 * starts again from setup once everything is deleted.
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenCredits: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.task) { if (uiState.task == DataTask.DELETED) onDeleted() }
    SettingsScreen(
        uiState = uiState,
        actions = SettingsActions(
            onLeadIn = viewModel::setLeadInSeconds,
            onSounds = viewModel::setSounds,
            onBuzzes = viewModel::setBuzzes,
            onExport = viewModel::export,
            onDeleteEverything = viewModel::deleteEverything,
        ),
        onBack = onBack,
        onOpenCredits = onOpenCredits,
    )
}

/**
 * Settings, as the Settings artboard, with only what works today: the
 * count-in, sounds and buzzes, the person's data, and about.
 */
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    actions: SettingsActions,
    onBack: () -> Unit,
    onOpenCredits: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    // The system's save dialog: the person picks where the file goes, so no storage permission is needed.
    val saveExport =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            uri?.let { actions.onExport(it.toString()) }
        }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackHeader(stringResource(R.string.settings_title), onBack)
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Training(uiState, actions)
                Sounds(uiState, actions)
                YourData(
                    task = uiState.task,
                    onExport = { saveExport.launch(EXPORT_FILE_NAME) },
                    onDelete = { confirming = true },
                )
                About(onOpenCredits)
            }
        }
        if (confirming) {
            ConfirmSheet(
                title = stringResource(R.string.settings_delete_title),
                body = stringResource(R.string.settings_delete_body),
                keepLabel = stringResource(R.string.settings_delete_keep),
                confirmLabel = stringResource(R.string.settings_delete_confirm),
                onKeep = { confirming = false },
                onConfirm = {
                    confirming = false
                    actions.onDeleteEverything()
                },
            )
        }
    }
}

/** The count-in before holds and negatives. */
@Composable
private fun Training(state: SettingsUiState, actions: SettingsActions) {
    Group(stringResource(R.string.settings_training))
    Column(modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 12.dp)) {
        RowText(stringResource(R.string.settings_lead_in), stringResource(R.string.settings_lead_in_note))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 12.dp).selectableGroup(),
        ) {
            LEAD_IN_CHOICES.forEach { seconds ->
                val selected = state.settings.leadInSeconds == seconds
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(if (selected) LevitxColors.Text else LevitxColors.Background)
                        .border(1.dp, if (selected) LevitxColors.Text else LevitxColors.Outline)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = {
                            actions.onLeadIn(seconds)
                        }),
                ) {
                    Text(
                        text = stringResource(R.string.settings_seconds, seconds),
                        style = LevitxType.Caption,
                        color = if (selected) LevitxColors.Background else LevitxColors.Text,
                    )
                }
            }
        }
    }
    Rule()
}

/** Beeps and buzzes for timers and rest. */
@Composable
private fun Sounds(state: SettingsUiState, actions: SettingsActions) {
    Group(stringResource(R.string.settings_sounds_group))
    SwitchRow(
        title = stringResource(R.string.settings_sounds),
        note = stringResource(R.string.settings_sounds_note),
        checked = state.settings.sounds,
        onChange = actions.onSounds,
    )
    SwitchRow(
        title = stringResource(R.string.settings_buzzes),
        note = stringResource(R.string.settings_buzzes_note),
        checked = state.settings.buzzes,
        onChange = actions.onBuzzes,
    )
}

/** Where the data lives, the export, and deleting everything. */
@Composable
private fun YourData(task: DataTask, onExport: () -> Unit, onDelete: () -> Unit) {
    Group(stringResource(R.string.settings_data))
    Text(
        text = stringResource(R.string.settings_data_note),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = LevitxColors.TextSoft,
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 12.dp),
    )
    DataButton(
        stringResource(R.string.settings_export),
        stringResource(R.string.settings_export_kind),
        task != DataTask.EXPORTING,
        onExport,
    )
    DataButton(
        stringResource(R.string.settings_delete),
        stringResource(R.string.settings_delete_kind),
        task != DataTask.DELETING,
        onDelete,
    )
    taskMessage(task)?.let { message ->
        Text(
            text = message,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = if (task == DataTask.EXPORT_FAILED ||
                task == DataTask.DELETE_FAILED
            ) {
                LevitxColors.Accent
            } else {
                LevitxColors.TextMuted
            },
            modifier = Modifier
                .padding(start = ScreenPadding, end = ScreenPadding, top = 10.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    Rule(Modifier.padding(top = 18.dp))
}

/** Photo credits and the version. */
@Composable
private fun About(onOpenCredits: () -> Unit) {
    Group(stringResource(R.string.settings_about))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(role = Role.Button, onClick = onOpenCredits)
            .padding(horizontal = ScreenPadding),
    ) {
        Text(stringResource(R.string.settings_credits), fontSize = 15.sp, color = LevitxColors.Text)
    }
    Rule(Modifier.padding(horizontal = ScreenPadding))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = ScreenPadding)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(
            stringResource(R.string.settings_version),
            fontSize = 15.sp,
            color = LevitxColors.Text,
            modifier = Modifier.weight(1f),
        )
        Caption(BuildConfig.VERSION_NAME)
    }
}
