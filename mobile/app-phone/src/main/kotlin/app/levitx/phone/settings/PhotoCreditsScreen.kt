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
    Credit(R.string.credit_first_push_up, "Monika Kab", "https://unsplash.com/photos/JeCVBSpS3xU"),
    Credit(R.string.credit_twenty_push_ups, "Gordon Cowie", "https://unsplash.com/photos/ISg1JhN_vFk"),
    Credit(R.string.credit_first_dip, "Vitaly Gariev", "https://unsplash.com/photos/u8wayDt6GqU"),
    Credit(R.string.credit_ten_pull_ups, "Shoham Avisrur", "https://unsplash.com/photos/gywolEHDUqg"),
    Credit(R.string.credit_l_sit, "Pedro Araújo", "https://unsplash.com/photos/S6zR6btkG-M"),
    Credit(R.string.credit_pistol_squat, "GMB Fitness", "https://unsplash.com/photos/9v1rPKsE4cU"),
    Credit(R.string.credit_shrimp_squat, "Pavigym Prama", "https://unsplash.com/photos/PCxkBX24sWo"),
    Credit(R.string.credit_handstand, "GMB Fitness", "https://unsplash.com/photos/c0Kz-JHWGmQ"),
    Credit(R.string.credit_nordic_curl, "Shoham Avisrur", "https://unsplash.com/photos/lVNmm3RhMBc"),
    Credit(R.string.credit_dragon_flag, "Ayo Ogunseinde", "https://unsplash.com/photos/3GFOT3sgg4Y"),
    Credit(R.string.credit_handstand_push_up, "GMB Fitness", "https://unsplash.com/photos/HxzncziymiM"),
    Credit(R.string.credit_back_lever, "Kate Trysh", "https://unsplash.com/photos/ZaFT5en14pY"),
    Credit(R.string.credit_one_arm_push_up, "Fortune Vieyra", "https://unsplash.com/photos/jD4MtXnsJ6w"),
    Credit(R.string.credit_tuck_planche, "Bogdan Pasca", "https://unsplash.com/photos/UBC4m6KEWcQ"),
    Credit(R.string.credit_front_lever, "Kate Trysh", "https://unsplash.com/photos/WnXzlISbtY0"),
    Credit(R.string.credit_one_arm_pull_up, "Amanda Hodge", "https://unsplash.com/photos/Wqn_gieaKyI"),
    Credit(R.string.credit_cardio, "Element5 Digital", "https://unsplash.com/photos/7QYd1VxLRbM"),
    Credit(R.string.credit_mobility, "Alora Griffiths", "https://unsplash.com/photos/WX7FSaiYxK8"),
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
