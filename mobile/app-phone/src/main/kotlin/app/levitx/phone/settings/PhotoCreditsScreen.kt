package app.levitx.phone.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.BackHeader
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding

/**
 * One photo in the app.
 *
 * @property usedFor Where it is used.
 * @property photographer Who took it, or null when Unsplash names them on the photo's page only.
 * @property url The photo's page on Unsplash.
 */
private data class Credit(@param:StringRes val usedFor: Int, val photographer: String?, val url: String)

// Kept in step with docs/app/PHOTO-CREDITS.md.
private val credits = listOf(
    Credit(R.string.credit_welcome, "Shoham Avisrur", "https://unsplash.com/photos/3HnxyD3J1Qg"),
    Credit(R.string.credit_first_pull_up, "Anastase Maragos", "https://unsplash.com/photos/IZw5zLHMfd4"),
    Credit(R.string.credit_muscle_up, "Kate Trysh", "https://unsplash.com/photos/A2OmNsPhVj4"),
    Credit(R.string.credit_push, null, "https://unsplash.com/photos/qlid9Lys8r0"),
    Credit(R.string.credit_upper, null, "https://unsplash.com/photos/fG0p4Qh_aWI"),
    Credit(R.string.credit_legs, null, "https://unsplash.com/photos/928VXYtfsRI"),
    Credit(R.string.credit_core, null, "https://unsplash.com/photos/Pu7BOaYlYGY"),
    Credit(R.string.credit_full_body, null, "https://unsplash.com/photos/QwAJTPXbj88"),
)

/** The photos in the app and who took them. Each opens its page on Unsplash in the browser. */
@Composable
fun PhotoCreditsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        BackHeader(stringResource(R.string.settings_credits), onBack)
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Lede(stringResource(R.string.credits_intro), Modifier.padding(ScreenPadding))
            Rule()
            credits.forEach { credit ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clickable(role = Role.Button, onClick = { uriHandler.openUri(credit.url) })
                        .padding(horizontal = ScreenPadding, vertical = 10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = credit.photographer ?: stringResource(R.string.credits_on_unsplash),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = LevitxColors.Text,
                        )
                        Caption(stringResource(credit.usedFor), color = LevitxColors.TextFaint)
                    }
                    Icon(
                        painterResource(R.drawable.ic_chevron_forward),
                        contentDescription = null,
                        tint = LevitxColors.TextFaint,
                    )
                }
                Rule(Modifier.padding(horizontal = ScreenPadding))
            }
        }
    }
}
