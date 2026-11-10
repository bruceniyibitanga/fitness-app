package app.levitx.phone.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxFonts

/**
 * Material 3 theme for the phone, built from the shared design tokens.
 *
 * Material components (buttons, sheets, dialogs) pick up the Levitx colours,
 * fonts and square edges automatically, so screens rarely need to style
 * anything by hand.
 */
@Composable
fun LevitxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LevitxColorScheme,
        typography = LevitxTypography,
        shapes = LevitxShapes,
        content = content,
    )
}

private val LevitxColorScheme = darkColorScheme(
    primary = LevitxColors.Accent,
    onPrimary = LevitxColors.Background,
    secondary = LevitxColors.Ice,
    onSecondary = LevitxColors.Background,
    background = LevitxColors.Background,
    onBackground = LevitxColors.Text,
    surface = LevitxColors.Background,
    onSurface = LevitxColors.Text,
    surfaceVariant = LevitxColors.Surface,
    onSurfaceVariant = LevitxColors.TextSoft,
    surfaceContainer = LevitxColors.Surface,
    outline = LevitxColors.Line,
    outlineVariant = LevitxColors.Tick,
)

/** Material's type scale with Archivo for text and JetBrains Mono for labels. */
private val LevitxTypography: Typography = Typography().let { base ->
    fun TextStyle.archivo() = copy(fontFamily = LevitxFonts.Archivo)
    fun TextStyle.mono() = copy(fontFamily = LevitxFonts.JetBrainsMono)
    Typography(
        displayLarge = base.displayLarge.archivo(),
        displayMedium = base.displayMedium.archivo(),
        displaySmall = base.displaySmall.archivo(),
        headlineLarge = base.headlineLarge.archivo(),
        headlineMedium = base.headlineMedium.archivo(),
        headlineSmall = base.headlineSmall.archivo(),
        titleLarge = base.titleLarge.archivo(),
        titleMedium = base.titleMedium.archivo(),
        titleSmall = base.titleSmall.archivo(),
        bodyLarge = base.bodyLarge.archivo(),
        bodyMedium = base.bodyMedium.archivo(),
        bodySmall = base.bodySmall.archivo(),
        labelLarge = base.labelLarge.mono(),
        labelMedium = base.labelMedium.mono(),
        labelSmall = base.labelSmall.mono(),
    )
}

/** Square edges everywhere, matching the brand. */
private val LevitxShapes = RoundedCornerShape(0.dp).let { square ->
    Shapes(
        extraSmall = square,
        small = square,
        medium = square,
        large = square,
        extraLarge = square,
    )
}
