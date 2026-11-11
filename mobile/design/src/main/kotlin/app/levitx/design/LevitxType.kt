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

    /** The one huge number on a screen, like Today's "3×8". */
    val Hero = TextStyle(
        fontFamily = LevitxFonts.ArchivoCompressed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 112.sp,
        lineHeight = 92.sp,
        letterSpacing = (-0.02).em,
    )

    /** Poster headline for welcome and plan-ready screens. */
    val Poster = TextStyle(
        fontFamily = LevitxFonts.ArchivoCompressed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 52.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.01).em,
    )

    /** A screen's title. */
    val Title = TextStyle(
        fontFamily = LevitxFonts.ArchivoCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.01).em,
    )

    /** A figure in a stat strip or at the end of a list row. */
    val Figure = TextStyle(
        fontFamily = LevitxFonts.ArchivoSemiCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 26.sp,
    )

    /** Small mono caps for section headers and captions, like "WHY THIS SESSION". */
    val Caption = TextStyle(
        fontFamily = LevitxFonts.JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.14.em,
    )

    /** The smallest mono caps, under a figure, like "KG PER REP". */
    val Micro = TextStyle(
        fontFamily = LevitxFonts.JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.14.em,
    )
}
