package app.levitx.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import app.levitx.core.load.WeightUnit
import app.levitx.data.settings.SettingsRepository
import app.levitx.wear.navigation.LevitxWearNavDisplay
import app.levitx.wear.theme.LevitxWearTheme
import app.levitx.wear.ui.LocalWeightUnit
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.map

/**
 * The watch app's only activity. Every screen is a Compose destination inside
 * it.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** Where the kg or lb choice synced from the phone is kept. */
    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // The phone's kg or lb choice, copied here by sync. Kilograms until it's read.
            val unitsFlow = remember { settingsRepository.observe().map { it.units } }
            val units by unitsFlow.collectAsStateWithLifecycle(initialValue = WeightUnit.KG)
            CompositionLocalProvider(LocalWeightUnit provides units) {
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
}
