package app.levitx.design

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Brand text styles with no direct Material equivalent: the hero headline,
 * mono labels and numbers. Everything else comes from each app's Material
 * typography, which the app themes set to the Levitx fonts.
 *
 * Sizes are in `sp`, so they follow the system font size setting.
 */
object LevitxType {
    /** Big, tight headline for the one thing a screen is about. */
    val Display = TextStyle(
        fontFamily = LevitxFonts.Archivo,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.02).em,
    )

    /** Small uppercase labels above values, like "CURRENT GATE". */
    val Label = TextStyle(
        fontFamily = LevitxFonts.JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.08.em,
    )

    /** Numbers: reps, kilograms, timers. Every digit is the same width. */
    val Numeral = TextStyle(
        fontFamily = LevitxFonts.JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
    )
}
