package app.levitx.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.levitx.core.coach.BodyArea
import app.levitx.core.pain.PainAdvice
import app.levitx.core.pain.RedFlag
import app.levitx.phone.pain.PainCheckActions
import app.levitx.phone.pain.PainCheckScreen
import app.levitx.phone.pain.PainCheckUiState
import app.levitx.phone.pain.PainStep
import app.levitx.phone.theme.LevitxTheme
import com.android.tools.screenshot.PreviewTest

/*
 * The pain check, rendered by CI to check against the "Pain check: where",
 * "Warning-sign screen" and "Pain check-in" artboards.
 */

private val sore = mapOf(BodyArea.ELBOW to PainAdvice.EASE_OFF, BodyArea.KNEE to PainAdvice.SEE_PROFESSIONAL)

@Composable
private fun PainCheck(state: PainCheckUiState.Ready) {
    LevitxTheme { PainCheckScreen(uiState = state, actions = PainCheckActions.None) }
}

@PreviewTest
@Preview(name = "45 Pain check, where", widthDp = 390, heightDp = 844)
@Composable
fun PainAreaScreenshot() {
    PainCheck(PainCheckUiState.Ready(step = PainStep.AREA, sore = sore))
}

@PreviewTest
@Preview(name = "46 Pain check, warning signs", widthDp = 390, heightDp = 844)
@Composable
fun PainSignsScreenshot() {
    PainCheck(PainCheckUiState.Ready(step = PainStep.SIGNS, sore = emptyMap(), area = BodyArea.SHOULDER))
}

@PreviewTest
@Preview(name = "47 Pain check, a warning sign on", widthDp = 390, heightDp = 844)
@Composable
fun PainSignsOnScreenshot() {
    PainCheck(
        PainCheckUiState.Ready(
            step = PainStep.SIGNS,
            sore = emptyMap(),
            area = BodyArea.SHOULDER,
            signs = setOf(RedFlag.NUMBNESS_OR_TINGLING),
        ),
    )
}

@PreviewTest
@Preview(name = "48 Pain check, nothing picked", widthDp = 390, heightDp = 844)
@Composable
fun PainRatingEmptyScreenshot() {
    PainCheck(PainCheckUiState.Ready(step = PainStep.RATING, sore = emptyMap(), area = BodyArea.SHOULDER))
}

@PreviewTest
@Preview(name = "49 Pain check, within the guide", widthDp = 390, heightDp = 844)
@Composable
fun PainRatingGuideScreenshot() {
    PainCheck(PainCheckUiState.Ready(step = PainStep.RATING, sore = emptyMap(), area = BodyArea.SHOULDER, rating = 4))
}

@PreviewTest
@Preview(name = "50 Pain check, over the guide", widthDp = 390, heightDp = 844)
@Composable
fun PainRatingOverScreenshot() {
    PainCheck(PainCheckUiState.Ready(step = PainStep.RATING, sore = emptyMap(), area = BodyArea.SHOULDER, rating = 7))
}
