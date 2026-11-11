package app.levitx.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType

/*
 * Text pieces from the design: mono captions, titles, ledes, help lines and
 * the 1 dp rules between sections.
 */

/** Mono caps over a section, like "SESSION · 4 BLOCKS". */
@Composable
fun Caption(text: String, modifier: Modifier = Modifier, color: Color = LevitxColors.TextMuted) {
    Text(text = text.uppercase(), style = LevitxType.Caption, color = color, modifier = modifier)
}

/** A screen's title, read out as a heading. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = LevitxType.Title,
        color = LevitxColors.Text,
        modifier = modifier.semantics { heading() },
    )
}

/** Muted body text under a title. */
@Composable
fun Lede(text: String, modifier: Modifier = Modifier) {
    Text(text = text, fontSize = 14.sp, lineHeight = 20.sp, color = LevitxColors.TextMuted, modifier = modifier)
}

/** Small muted help under a control. */
@Composable
fun Help(text: String, modifier: Modifier = Modifier) {
    Text(text = text, fontSize = 12.sp, lineHeight = 17.sp, color = LevitxColors.TextMuted, modifier = modifier)
}

/** The 1 dp rule between sections. */
@Composable
fun Rule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LevitxColors.Line),
    )
}
