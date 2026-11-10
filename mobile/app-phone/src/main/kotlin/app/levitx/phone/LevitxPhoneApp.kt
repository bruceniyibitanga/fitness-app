package app.levitx.phone

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Placeholder brand colours. Phase 1 replaces these with the shared design
// tokens (see docs/app/ROADMAP.md).
private val Background = Color(0xFF0B0B0A)
private val Accent = Color(0xFFFF6A2B)
private val ScreenPadding = 24.dp

/** Root composable of the phone app. */
@Composable
fun LevitxPhoneApp(modifier: Modifier = Modifier) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = Background,
            surface = Background,
        ),
    ) {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            Text(
                text = stringResource(R.string.home_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(ScreenPadding),
            )
        }
    }
}

@Preview
@Composable
private fun LevitxPhoneAppPreview() {
    LevitxPhoneApp()
}
