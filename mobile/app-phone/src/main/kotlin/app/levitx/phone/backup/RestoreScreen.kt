package app.levitx.phone.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.data.backup.RecoveryWords
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.BackHeader
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.ConfirmSheet
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.SecureWindow

/** Height of the bar at the foot of each step. */
private val StepBarHeight = 64.dp

/** Height of the field for six words: room for two lines of them. */
private val WordsFieldHeight = 96.dp

/**
 * What the restore screen can do.
 *
 * @property onClose Leaves the screen.
 * @property onChoose Opens the system's file picker.
 * @property onRestore Restores with the words typed, once the person said yes.
 * @property onDone Leaves after a restore, for the restored plan.
 */
data class RestoreActions(
    val onClose: () -> Unit = {},
    val onChoose: () -> Unit = {},
    val onRestore: (String) -> Unit = {},
    val onDone: () -> Unit = {},
)

/**
 * The restore destination: connects [RestoreViewModel] to [RestoreScreen].
 * The window is kept out of screenshots while the words are typed.
 *
 * @param onRestored Leaves for the restored plan.
 */
@Composable
fun RestoreRoute(onClose: () -> Unit, onRestored: () -> Unit, viewModel: RestoreViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Any file: a backup has no type Android knows, and the cipher turns away anything else.
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { uri -> viewModel.chosen(uri.toString()) }
    }
    SecureWindow()
    RestoreScreen(
        uiState = uiState,
        actions = RestoreActions(
            onClose = onClose,
            onChoose = { open.launch(arrayOf("*/*")) },
            onRestore = viewModel::restore,
            onDone = onRestored,
        ),
    )
}

/** Restoring, as the "Restore a backup" artboard. */
@Composable
fun RestoreScreen(
    uiState: RestoreUiState,
    actions: RestoreActions,
    modifier: Modifier = Modifier,
    initialWords: String = "",
) {
    // The words are held in memory only, never in saved state: they unlock the person's health data.
    var typed by remember { mutableStateOf(initialWords) }
    var confirming by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackHeader(stringResource(R.string.restore_title), actions.onClose)
            when (uiState) {
                RestoreUiState.Choose -> ChooseStep(actions.onChoose)

                is RestoreUiState.Words -> WordsStep(typed, { typed = it }, uiState.issue) { confirming = true }

                RestoreUiState.Working ->
                    Caption(stringResource(R.string.restore_working), modifier = Modifier.padding(ScreenPadding))

                RestoreUiState.Restored -> DoneStep(actions.onDone)
            }
        }
        if (confirming) {
            ConfirmSheet(
                title = stringResource(R.string.restore_confirm_title),
                body = stringResource(R.string.restore_confirm_body),
                keepLabel = stringResource(R.string.restore_keep),
                confirmLabel = stringResource(R.string.restore_replace),
                onKeep = { confirming = false },
                onConfirm = {
                    confirming = false
                    actions.onRestore(typed)
                },
            )
        }
    }
}

/** Step 1: what restoring does, and the file picker. */
@Composable
private fun ColumnScope.ChooseStep(onChoose: () -> Unit) {
    Column(modifier = Modifier.weight(1f)) {
        StepTitle(stringResource(R.string.restore_label), stringResource(R.string.restore_choose_title))
        Spacer(Modifier.weight(1f))
    }
    AccentBar(stringResource(R.string.restore_choose), onChoose, height = StepBarHeight)
}

/** Step 2: the six words, how many are typed, and why the last try didn't work. */
@Composable
private fun ColumnScope.WordsStep(
    typed: String,
    onType: (String) -> Unit,
    issue: RestoreIssue?,
    onRestore: () -> Unit,
) {
    val label = stringResource(R.string.restore_words_title)
    val count = typed.lowercase().split(Regex("[\\s,]+")).count { it.isNotEmpty() }
    Column(modifier = Modifier.weight(1f)) {
        StepTitle(stringResource(R.string.restore_file_label), label, labelColor = LevitxColors.TextMuted)
        SecretField(
            value = typed,
            onValueChange = onType,
            label = label,
            wrong = issue != null,
            minHeight = WordsFieldHeight,
            singleLine = false,
            modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 18.dp),
        )
        Row(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 8.dp)) {
            Caption(
                issue?.let { issueText(it) }.orEmpty(),
                color = LevitxColors.AccentHover,
                modifier = Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            Caption(stringResource(R.string.restore_words_count, count, RecoveryWords.COUNT))
        }
    }
    AccentBar(
        stringResource(R.string.restore_go),
        onRestore,
        enabled = RecoveryWords.parse(typed) != null,
        height = StepBarHeight,
    )
}

/** Step 3: everything is back. */
@Composable
private fun ColumnScope.DoneStep(onDone: () -> Unit) {
    Column(modifier = Modifier.weight(1f)) {
        StepTitle(
            stringResource(R.string.restore_done_label),
            stringResource(R.string.restore_done_title),
            labelColor = LevitxColors.Ice,
        )
        Spacer(Modifier.weight(1f))
    }
    AccentBar(stringResource(R.string.restore_done), onDone, height = StepBarHeight)
}

/** Why the last try didn't restore, in a few words. */
@Composable
private fun issueText(issue: RestoreIssue): String = stringResource(
    when (issue) {
        RestoreIssue.WRONG_WORDS -> R.string.restore_issue_words
        RestoreIssue.NOT_A_BACKUP -> R.string.restore_issue_not_backup
        RestoreIssue.NEWER_VERSION -> R.string.restore_issue_newer
        RestoreIssue.UNUSABLE -> R.string.restore_issue_unusable
        RestoreIssue.TOO_LARGE -> R.string.restore_issue_large
        RestoreIssue.WORKOUT_IN_PROGRESS -> R.string.restore_issue_workout
        RestoreIssue.UNREADABLE_FILE -> R.string.restore_issue_read
    },
)
