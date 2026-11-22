package app.levitx.phone.backup

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.data.backup.RecoveryWords
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.BackHeader
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.SecureWindow
import java.time.LocalDate

/** Height of the bar at the foot of each step. */
private val StepBarHeight = 64.dp

/** The three steps, as counted on screen. */
private const val WORDS_STEP = 1
private const val CHECK_STEP = 2
private const val SAVED_STEP = 3

/**
 * What the backup screen can do.
 *
 * @property onClose Leaves the screen.
 * @property onWrittenDown Step 1 is done.
 * @property onShowWordsAgain Back from step 2 to the words.
 * @property matches True when the typed text is the word at that place.
 * @property onSave Opens the save dialog.
 */
data class BackupActions(
    val onClose: () -> Unit = {},
    val onWrittenDown: () -> Unit = {},
    val onShowWordsAgain: () -> Unit = {},
    val matches: (Int, String) -> Boolean = { _, _ -> false },
    val onSave: () -> Unit = {},
)

/**
 * The backup destination: connects [BackupViewModel] to [BackupScreen]. The
 * window is kept out of screenshots and the recent apps list while the words
 * are on screen or typed.
 */
@Composable
fun BackupRoute(onClose: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
        it?.let { uri -> viewModel.save(uri.toString()) }
    }
    val fileName = stringResource(R.string.backup_file_name, LocalDate.now().toString())
    SecureWindow()
    BackHandler(enabled = uiState is BackupUiState.Check) { viewModel.showWordsAgain() }
    BackupScreen(
        uiState = uiState,
        actions = BackupActions(
            onClose = onClose,
            onWrittenDown = viewModel::writtenDown,
            onShowWordsAgain = viewModel::showWordsAgain,
            matches = viewModel::matches,
            onSave = { save.launch(fileName) },
        ),
    )
}

/** Backing up, in three steps, as the "Back up: your six words" artboard. */
@Composable
fun BackupScreen(uiState: BackupUiState, actions: BackupActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        BackHeader(
            stringResource(R.string.backup_title),
            if (uiState is BackupUiState.Check) actions.onShowWordsAgain else actions.onClose,
        )
        when (uiState) {
            BackupUiState.Preparing -> Unit

            is BackupUiState.Words -> WordsStep(uiState.words, actions.onWrittenDown)

            is BackupUiState.Check -> CheckStep(uiState, actions)

            BackupUiState.Saving -> Caption(
                stringResource(R.string.backup_saving),
                modifier = Modifier.padding(ScreenPadding),
            )

            BackupUiState.Saved -> SavedStep(actions.onClose)
        }
    }
}

/** Step 1: the six words in two columns, and the warning. */
@Composable
private fun ColumnScope.WordsStep(words: List<String>, onWrittenDown: () -> Unit) {
    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        StepTitle(stringResource(R.string.backup_step, WORDS_STEP), stringResource(R.string.backup_words_title))
        val rows = words.withIndex().chunked(2)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp),
        ) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (index, word) -> WordTile(index, word, Modifier.weight(1f)) }
                }
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(ScreenPadding)
                .fillMaxWidth()
                .border(1.dp, LevitxColors.Accent)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Caption(stringResource(R.string.backup_write_label), color = LevitxColors.AccentHover)
            Text(
                stringResource(R.string.backup_write_body),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = LevitxColors.Text,
            )
        }
    }
    AccentBar(stringResource(R.string.backup_written), onWrittenDown, height = StepBarHeight)
}

/** One word with its place, read out as "word 3, pillow". */
@Composable
private fun WordTile(index: Int, word: String, modifier: Modifier = Modifier) {
    val place = (index + 1).toString().padStart(2, '0')
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .height(WordHeight)
            .background(LevitxColors.Surface)
            .padding(horizontal = 14.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Caption(place, color = LevitxColors.TextFaint)
        Text(word, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LevitxColors.Text)
    }
}

/** Step 2: type two of the words back. Saving opens once both are right. */
@Composable
private fun ColumnScope.CheckStep(state: BackupUiState.Check, actions: BackupActions) {
    // Held in memory only: these are part of the secret.
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val firstRight = actions.matches(state.first, first)
    val secondRight = actions.matches(state.second, second)
    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        StepTitle(stringResource(R.string.backup_step, CHECK_STEP), stringResource(R.string.backup_check_title))
        CheckField(state.first, first, { first = it }, firstRight, Modifier.padding(top = 22.dp))
        CheckField(state.second, second, { second = it }, secondRight, Modifier.padding(top = 14.dp))
        if (state.saveFailed) {
            Caption(
                stringResource(R.string.backup_save_failed),
                color = LevitxColors.AccentHover,
                modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 14.dp),
            )
        }
    }
    AccentBar(
        stringResource(R.string.backup_save),
        actions.onSave,
        enabled = firstRight && secondRight,
        height = StepBarHeight,
    )
}

/** "WORD 2" and its field: a mark once it's right, "NOT IT" for a real word that isn't. */
@Composable
private fun CheckField(index: Int, value: String, onValueChange: (String) -> Unit, right: Boolean, modifier: Modifier) {
    val label = stringResource(R.string.backup_word_place, index + 1)
    val wrong = !right && RecoveryWords.isWord(value)
    Column(modifier = modifier.padding(horizontal = ScreenPadding)) {
        Caption(label, modifier = Modifier.padding(bottom = 6.dp))
        SecretField(value, onValueChange, label, wrong) {
            when {
                right -> MatchMark()
                wrong -> Caption(stringResource(R.string.backup_word_wrong), color = LevitxColors.AccentHover)
            }
        }
    }
}

/** Step 3: saved, and two facts about the file. */
@Composable
private fun ColumnScope.SavedStep(onDone: () -> Unit) {
    Column(modifier = Modifier.weight(1f)) {
        StepTitle(stringResource(R.string.backup_step, SAVED_STEP), stringResource(R.string.backup_saved_title))
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 18.dp),
        ) {
            Fact(stringResource(R.string.backup_saved_encrypted))
            Fact(stringResource(R.string.backup_saved_cloud))
        }
        Spacer(Modifier.weight(1f))
    }
    AccentBar(stringResource(R.string.backup_done), onDone, height = StepBarHeight)
}
