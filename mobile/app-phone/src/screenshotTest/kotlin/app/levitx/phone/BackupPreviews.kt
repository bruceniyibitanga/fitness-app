package app.levitx.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.levitx.design.LevitxTheme
import app.levitx.phone.backup.BackupActions
import app.levitx.phone.backup.BackupScreen
import app.levitx.phone.backup.BackupUiState
import app.levitx.phone.backup.RestoreActions
import app.levitx.phone.backup.RestoreIssue
import app.levitx.phone.backup.RestoreScreen
import app.levitx.phone.backup.RestoreUiState
import com.android.tools.screenshot.PreviewTest

/** Sample words, all on the list. Never a real backup's. */
private val sampleWords = listOf("ladder", "gravity", "pliable", "stencil", "shrimp", "mascot")

@PreviewTest
@Preview(name = "55 Back up, your six words", widthDp = 390, heightDp = 844)
@Composable
fun BackupWordsScreenshot() {
    LevitxTheme { BackupScreen(BackupUiState.Words(sampleWords), BackupActions()) }
}

@PreviewTest
@Preview(name = "56 Back up, type two of them", widthDp = 390, heightDp = 844)
@Composable
fun BackupCheckScreenshot() {
    LevitxTheme { BackupScreen(BackupUiState.Check(first = 1, second = 4), BackupActions()) }
}

@PreviewTest
@Preview(name = "57 Back up, saved", widthDp = 390, heightDp = 844)
@Composable
fun BackupSavedScreenshot() {
    LevitxTheme { BackupScreen(BackupUiState.Saved, BackupActions()) }
}

@PreviewTest
@Preview(name = "58 Restore, choose the file", widthDp = 390, heightDp = 844)
@Composable
fun RestoreChooseScreenshot() {
    LevitxTheme { RestoreScreen(RestoreUiState.Choose, RestoreActions()) }
}

@PreviewTest
@Preview(name = "59 Restore, words that don't open it", widthDp = 390, heightDp = 844)
@Composable
fun RestoreWrongWordsScreenshot() {
    LevitxTheme {
        RestoreScreen(
            RestoreUiState.Words(RestoreIssue.WRONG_WORDS),
            RestoreActions(),
            initialWords = sampleWords.joinToString(" "),
        )
    }
}
