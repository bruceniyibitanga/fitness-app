package app.levitx.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material3.AppScaffold
import app.levitx.wear.navigation.LevitxWearNavDisplay
import app.levitx.wear.theme.LevitxWearTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The watch app's only activity. Every screen is a Compose destination inside
 * it.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LevitxWearTheme {
                // AppScaffold sits above navigation so the time text stays put
                // while screens swipe in and out beneath it.
                AppScaffold {
                    LevitxWearNavDisplay()
                }
            }
        }
    }
}
