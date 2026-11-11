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

    /** Archivo at 72% width: poster headlines and the big numbers. */
    val ArchivoCompressed: FontFamily = variableFamily(R.font.archivo, ArchivoWeights, width = COMPRESSED)

    /** Archivo at 78% width: screen titles. */
    val ArchivoCondensed: FontFamily = variableFamily(R.font.archivo, ArchivoWeights, width = CONDENSED)

    /** Archivo at 90% width: figures in stat strips and lists. */
    val ArchivoSemiCondensed: FontFamily = variableFamily(R.font.archivo, ArchivoWeights, width = SEMI_CONDENSED)
}

// Widths from the design artboards (CSS font-stretch), on Archivo's wdth axis (62 to 125).
private const val COMPRESSED = 72f
private const val CONDENSED = 78f
private const val SEMI_CONDENSED = 90f

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

private fun variableFamily(resId: Int, weights: List<FontWeight>, width: Float? = null): FontFamily = FontFamily(
    weights.map { weight ->
        val settings = if (width == null) {
            FontVariation.Settings(FontVariation.weight(weight.weight))
        } else {
            FontVariation.Settings(FontVariation.weight(weight.weight), FontVariation.width(width))
        }
        Font(resId = resId, weight = weight, variationSettings = settings)
    },
)
