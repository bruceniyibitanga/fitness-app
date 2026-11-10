package app.levitx.wear.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxFonts

/**
 * Wear OS Material 3 theme, built from the shared design tokens.
 *
 * Two deliberate differences from the phone:
 * - Shapes keep Wear's rounded defaults. Square corners fight a round screen
 *   and Wear OS guidance asks for shapes that follow the display.
 * - Body and label text keep the system font, which is tuned for small,
 *   glanceable text. Archivo is used for display and titles, and JetBrains
 *   Mono for numbers, so the brand still reads at a glance.
 */
@Composable
fun LevitxWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LevitxWearColorScheme,
        typography = LevitxWearTypography,
        content = content,
    )
}

private val LevitxWearColorScheme = ColorScheme(
    primary = LevitxColors.Accent,
    onPrimary = LevitxColors.Background,
    secondary = LevitxColors.Ice,
    onSecondary = LevitxColors.Background,
    surfaceContainer = LevitxColors.Surface,
    onSurface = LevitxColors.Text,
    onSurfaceVariant = LevitxColors.TextSoft,
    outline = LevitxColors.Line,
    background = LevitxColors.Background,
    onBackground = LevitxColors.Text,
)

/** Wear's type scale with Archivo titles and JetBrains Mono numerals. Other slots keep their defaults. */
private val LevitxWearTypography: Typography = Typography().let { base ->
    fun TextStyle.archivo() = copy(fontFamily = LevitxFonts.Archivo)
    fun TextStyle.mono() = copy(fontFamily = LevitxFonts.JetBrainsMono)
    Typography(
        displayLarge = base.displayLarge.archivo(),
        displayMedium = base.displayMedium.archivo(),
        displaySmall = base.displaySmall.archivo(),
        titleLarge = base.titleLarge.archivo(),
        titleMedium = base.titleMedium.archivo(),
        titleSmall = base.titleSmall.archivo(),
        numeralExtraLarge = base.numeralExtraLarge.mono(),
        numeralLarge = base.numeralLarge.mono(),
        numeralMedium = base.numeralMedium.mono(),
        numeralSmall = base.numeralSmall.mono(),
        numeralExtraSmall = base.numeralExtraSmall.mono(),
    )
}
