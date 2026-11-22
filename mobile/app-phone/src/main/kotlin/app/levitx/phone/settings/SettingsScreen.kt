package app.levitx.phone.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.load.WeightUnit
import app.levitx.data.settings.LEAD_IN_CHOICES
import app.levitx.design.LevitxColors
import app.levitx.phone.BuildConfig
import app.levitx.phone.R
import app.levitx.phone.ui.BackHeader
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.ConfirmSheet
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.unitLabel

/** The export's suggested file name. The person can change it in the save dialog. */
private const val EXPORT_FILE_NAME = "levitx-export.zip"

/**
 * What Settings can do.
 *
 * @property onLeadIn Sets the count-in, in seconds.
 * @property onUnits Shows weights in kilograms or pounds.
 * @property onSounds Turns beeps on or off.
 * @property onBuzzes Turns buzzes on or off.
 * @property onOpenNotifications Opens Android's notification settings for Levitx.
 * @property onExport Writes the export to the chosen file's uri.
 * @property onDeleteEverything Deletes everything, once confirmed.
 * @property onSendFeedback Opens an email draft to support. False when the phone has no email app.
 * @property onOpenBackup Opens the backup screen.
 * @property onOpenRestore Opens the restore screen.
 */
data class SettingsActions(
    val onLeadIn: (Int) -> Unit,
    val onUnits: (WeightUnit) -> Unit,
    val onSounds: (Boolean) -> Unit,
    val onBuzzes: (Boolean) -> Unit,
    val onOpenNotifications: () -> Unit,
    val onExport: (String) -> Unit,
    val onDeleteEverything: () -> Unit,
    val onSendFeedback: () -> Boolean,
    val onOpenBackup: () -> Unit = {},
    val onOpenRestore: () -> Unit = {},
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
    onOpenBackup: () -> Unit,
    onOpenRestore: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.task) { if (uiState.task == DataTask.DELETED) onDeleted() }
    val context = LocalContext.current
    var notificationsOn by remember { mutableStateOf(true) }
    // Read each time Settings comes back to the front: the person may have just changed it in Android's settings.
    LifecycleResumeEffect(context) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose {}
    }
    SettingsScreen(
        uiState = uiState,
        notificationsOn = notificationsOn,
        actions = SettingsActions(
            onLeadIn = viewModel::setLeadInSeconds,
            onUnits = viewModel::setUnits,
            onSounds = viewModel::setSounds,
            onBuzzes = viewModel::setBuzzes,
            onOpenNotifications = { openNotificationSettings(context) },
            onExport = viewModel::export,
            onDeleteEverything = viewModel::deleteEverything,
            onSendFeedback = { sendFeedback(context) },
            onOpenBackup = onOpenBackup,
            onOpenRestore = onOpenRestore,
        ),
        onBack = onBack,
        onOpenCredits = onOpenCredits,
    )
}

/**
 * Settings, as the Settings artboard, with only what works today: the
 * count-in, sounds, buzzes and the workout notification, the person's data,
 * and about. [notificationsOn] is whether Android lets Levitx notify.
 */
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    notificationsOn: Boolean,
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
                Sounds(uiState, notificationsOn, actions)
                YourData(
                    task = uiState.task,
                    actions = actions,
                    onExport = { saveExport.launch(EXPORT_FILE_NAME) },
                    onDelete = { confirming = true },
                )
                About(onOpenCredits, actions.onSendFeedback)
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

/** The count-in before holds and negatives, and the unit weights are shown in. */
@Composable
private fun Training(state: SettingsUiState, actions: SettingsActions) {
    Group(stringResource(R.string.settings_training))
    Column(modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 12.dp)) {
        RowText(stringResource(R.string.settings_lead_in), stringResource(R.string.settings_lead_in_note))
        ChoiceBoxes(
            choices = LEAD_IN_CHOICES,
            selected = state.settings.leadInSeconds,
            label = { stringResource(R.string.settings_seconds, it) },
            onSelect = actions.onLeadIn,
        )
        RowText(
            stringResource(R.string.settings_units),
            stringResource(R.string.settings_units_note),
            modifier = Modifier.padding(top = 22.dp),
        )
        ChoiceBoxes(
            choices = WeightUnit.entries,
            selected = state.settings.units,
            label = { unitLabel(it).uppercase() },
            onSelect = actions.onUnits,
        )
    }
    Rule()
}

/** Beeps and buzzes for timers and rest, and the workout notification. */
@Composable
private fun Sounds(state: SettingsUiState, notificationsOn: Boolean, actions: SettingsActions) {
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
    LinkRow(
        title = stringResource(R.string.settings_notification),
        note = stringResource(R.string.settings_notification_note),
        status = stringResource(if (notificationsOn) R.string.settings_on else R.string.settings_off),
        onClick = actions.onOpenNotifications,
    )
}

/**
 * Android's notification settings for Levitx, where the workout notification
 * is turned on or off. Nothing happens on the rare phone without that screen.
 */
private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // A phone maker removed the screen. The person can still find it in Android's own settings.
    }
}

/** Where the data lives, backing up and restoring, the export, and deleting everything. */
@Composable
private fun YourData(task: DataTask, actions: SettingsActions, onExport: () -> Unit, onDelete: () -> Unit) {
    Group(stringResource(R.string.settings_data))
    Text(
        text = stringResource(R.string.settings_data_note),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = LevitxColors.TextSoft,
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 12.dp),
    )
    DataButton(
        stringResource(R.string.settings_backup),
        stringResource(R.string.settings_backup_kind),
        true,
        actions.onOpenBackup,
    )
    DataButton(
        stringResource(R.string.settings_restore),
        stringResource(R.string.settings_restore_kind),
        true,
        actions.onOpenRestore,
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

/** Sending feedback, photo credits and the version. */
@Composable
private fun About(onOpenCredits: () -> Unit, onSendFeedback: () -> Boolean) {
    // Set when the phone had no email app to open, so the address is shown to write to by hand.
    var noEmailApp by rememberSaveable { mutableStateOf(false) }
    Group(stringResource(R.string.settings_about))
    LinkRow(
        title = stringResource(R.string.settings_feedback),
        note = stringResource(R.string.settings_feedback_note),
        status = stringResource(R.string.settings_feedback_kind),
        onClick = { noEmailApp = !onSendFeedback() },
    )
    if (noEmailApp) {
        Text(
            text = stringResource(R.string.settings_feedback_no_app, FEEDBACK_ADDRESS),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = LevitxColors.Accent,
            modifier = Modifier
                .padding(horizontal = ScreenPadding, vertical = 10.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        Rule(Modifier.padding(horizontal = ScreenPadding))
    }
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
