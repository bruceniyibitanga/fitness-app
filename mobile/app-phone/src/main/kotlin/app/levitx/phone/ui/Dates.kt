package app.levitx.phone.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import app.levitx.core.load.formatKg
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

/*
 * Dates and signed figures as the progress and history screens show them, in
 * the phone's time zone and language.
 */

/** "18 Aug". */
@Composable
fun shortDate(epochMillis: Long): String = formatted(epochMillis, "d MMM")

/** "Tue 10 Nov". */
@Composable
fun dayDate(epochMillis: Long): String = formatted(epochMillis, "EEE d MMM")

/** "17:42", or "5:42 pm", as the phone's language writes times. */
@Composable
fun clockTime(epochMillis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }
    return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}

/** A change in kilograms with its sign: "+18.4", "−0.6", or "0". */
fun signedKg(kg: Double): String = when {
    kg >= TENTH -> "+" + formatKg(kg)
    kg <= -TENTH -> "−" + formatKg(abs(kg))
    else -> "0"
}

@Composable
private fun formatted(epochMillis: Long, pattern: String): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale, pattern) { DateTimeFormatter.ofPattern(pattern, locale) }
    return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}

/** Changes smaller than this round to nothing at one decimal place. */
private const val TENTH = 0.05
