package app.levitx.phone.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Photo
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.Wordmark

/** Where the welcome photo starts fading into the background, as a fraction of the screen. */
private const val WELCOME_FADE_START = 0.35f

/** How dark the top of the welcome photo is, so the wordmark reads over a bright sky. */
private const val WELCOME_TOP_SHADE = 0.35f

/** The first screen: one photo, the promise, one line about it, and "Build my plan". */
@Composable
internal fun WelcomeScreen(onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Photo(R.drawable.photo_welcome, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to LevitxColors.Background.copy(alpha = WELCOME_TOP_SHADE),
                        WELCOME_FADE_START to LevitxColors.Background.copy(alpha = 0f),
                        1f to LevitxColors.Background,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Wordmark(
                modifier = Modifier
                    .height(HeaderHeight)
                    .padding(horizontal = ScreenPadding),
            )
            Spacer(Modifier.weight(1f))
            WelcomeWords()
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, bottom = ScreenPadding),
            ) {
                AccentBar(text = stringResource(R.string.welcome_start), onClick = onStart)
                Text(
                    text = stringResource(R.string.welcome_private),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = LevitxColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** The promise, and one line on what Levitx does about it. */
@Composable
private fun WelcomeWords() {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, bottom = 28.dp),
    ) {
        Text(
            text = stringResource(R.string.welcome_title),
            style = LevitxType.Poster,
            color = LevitxColors.Text,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.welcome_body),
            fontSize = 16.sp,
            lineHeight = 23.sp,
            color = LevitxColors.TextSoft,
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun WelcomePreview() {
    LevitxTheme { WelcomeScreen(onStart = {}) }
}
