package app.levitx.design

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

/**
 * The two Levitx typefaces, bundled as variable fonts.
 *
 * One variable font file covers every weight, so the apps carry two files
 * instead of eight. Each [Font] entry below picks a weight from the file.
 */
object LevitxFonts {
    /** Archivo: headings and body text. */
    val Archivo: FontFamily = variableFamily(R.font.archivo, ArchivoWeights)

    /** JetBrains Mono: numbers, labels and anything that should line up. */
    val JetBrainsMono: FontFamily = variableFamily(R.font.jetbrains_mono, MonoWeights)
}

private val ArchivoWeights = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold,
)

private val MonoWeights = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.Bold,
)

private fun variableFamily(resId: Int, weights: List<FontWeight>): FontFamily = FontFamily(
    weights.map { weight ->
        Font(
            resId = resId,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)
