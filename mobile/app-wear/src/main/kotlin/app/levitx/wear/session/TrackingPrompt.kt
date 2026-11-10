package app.levitx.wear.session

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Text
import app.levitx.wear.R
import app.levitx.wear.ui.CenteredText
import app.levitx.wear.ui.SectionLabel

/**
 * Asks whether the watch may read heart rate, straight before the first
 * session that would use it, and says what it is for and what happens to it.
 *
 * "Not now" is a real choice: the session runs the same without it, and the
 * question isn't asked again until the app is next opened.
 */
@Composable
internal fun TrackingPrompt(onAllow: () -> Unit, onSkip: () -> Unit) {
    SessionList {
        item { SectionLabel(stringResource(R.string.tracking_label)) }
        item { CenteredText(stringResource(R.string.tracking_body)) }
        item {
            Button(
                onClick = onAllow,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.tracking_allow)) },
            )
        }
        item {
            FilledTonalButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.tracking_skip)) },
            )
        }
    }
}
