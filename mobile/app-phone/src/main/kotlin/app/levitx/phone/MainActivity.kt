package app.levitx.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * The phone app's only activity. Every screen is a Compose destination inside
 * it, which keeps navigation, state and transitions in one place.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars, as Android 15+ requires
        // for apps targeting it. Compose insets keep content clear of them.
        enableEdgeToEdge()
        setContent { LevitxPhoneApp() }
    }
}
