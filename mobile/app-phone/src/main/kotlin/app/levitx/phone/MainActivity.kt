package app.levitx.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.levitx.phone.navigation.LevitxNavDisplay
import app.levitx.phone.theme.LevitxTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The phone app's only activity. Every screen is a Compose destination inside
 * it, which keeps navigation, state and transitions in one place.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars, as Android 15+ requires
        // for apps targeting it. Screens use Scaffold insets to stay clear.
        enableEdgeToEdge()
        setContent {
            LevitxTheme {
                LevitxNavDisplay()
            }
        }
    }
}
