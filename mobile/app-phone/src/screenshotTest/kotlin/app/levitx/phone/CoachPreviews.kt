package app.levitx.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.levitx.design.LevitxColors
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.today.WeighInAsk
import app.levitx.phone.today.WeighInSheet
import com.android.tools.screenshot.PreviewTest

/*
 * The coach's prompts, rendered by CI to check against the "Coach" artboards.
 */

/** 6 Nov 2026, 07:00 UTC. */
private const val LAST_READING_AT = 1_794_034_800_000L

@PreviewTest
@Preview(name = "41 Weigh-in", widthDp = 390, heightDp = 844)
@Composable
fun WeighInScreenshot() {
    LevitxTheme {
        Box(modifier = Modifier.fillMaxSize().background(LevitxColors.Background)) {
            WeighInSheet(WeighInAsk(lastKg = 74.2, lastAtEpochMillis = LAST_READING_AT), onSave = {
            }, onSkip = {}, onDismiss = {})
        }
    }
}

@PreviewTest
@Preview(name = "42 Weigh-in, first time", widthDp = 390, heightDp = 844)
@Composable
fun FirstWeighInScreenshot() {
    LevitxTheme {
        Box(modifier = Modifier.fillMaxSize().background(LevitxColors.Background)) {
            WeighInSheet(WeighInAsk(lastKg = null, lastAtEpochMillis = null), onSave = {}, onSkip = {}, onDismiss = {})
        }
    }
}
