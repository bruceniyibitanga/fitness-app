package app.levitx.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.load.WeightUnit
import app.levitx.data.settings.SettingsRepository
import app.levitx.phone.navigation.LevitxNavDisplay
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.LocalWeightUnit
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * The phone app's only activity. Every screen is a Compose destination inside
 * it, which keeps navigation, state and transitions in one place.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** For the unit weights are shown in. Set by Hilt. */
    @Inject
    lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars, as Android 15+ requires
        // for apps targeting it. Screens use Scaffold insets to stay clear.
        enableEdgeToEdge()
        // Settings that can't be read show kilograms; nothing else depends on them.
        val units = settings.observe().map { it.units }.catch { emit(WeightUnit.KG) }
        setContent {
            val unit by units.collectAsStateWithLifecycle(initialValue = WeightUnit.KG)
            LevitxTheme {
                CompositionLocalProvider(LocalWeightUnit provides unit) {
                    LevitxNavDisplay()
                }
            }
        }
    }
}
