package app.levitx.phone.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/**
 * How Levitx moves between screens, in one place so every screen moves the
 * same way.
 *
 * A new screen slides in a short way and fades in while the old one fades
 * out; going back does the same the other way round. Nothing bounces, loops
 * or draws attention to itself. Compose scales these by Android's "animator
 * duration scale", so with animations turned off in the system settings
 * every change is a cut.
 */
object LevitxMotion {
    /** How long a move between screens takes, in milliseconds. */
    const val DURATION_MILLIS = 320

    /** The old screen is gone this soon, so two screens of text never sit on top of each other for long. */
    private const val FADE_OUT_MILLIS = 90

    /** Screens travel a tenth of the screen's width: enough to give a direction, not enough to feel like a swipe. */
    private const val SLIDE_DIVISOR = 10

    /** Moving on: to a deeper screen, or the next step. */
    fun forward(): ContentTransform = enter(direction = 1) togetherWith exit(direction = -1)

    /** Going back: to the screen before, or the step before. */
    fun back(): ContentTransform = enter(direction = -1) togetherWith exit(direction = 1)

    /** Changing between things of equal rank, such as the tabs: a plain cross-fade. */
    fun fade(): ContentTransform =
        fadeIn(tween(DURATION_MILLIS, easing = LinearOutSlowInEasing)) togetherWith fadeOut(tween(FADE_OUT_MILLIS))

    private fun enter(direction: Int): EnterTransition =
        slideInHorizontally(tween(DURATION_MILLIS, easing = FastOutSlowInEasing)) { direction * it / SLIDE_DIVISOR } +
            fadeIn(
                tween(DURATION_MILLIS - FADE_OUT_MILLIS, delayMillis = FADE_OUT_MILLIS, easing = LinearOutSlowInEasing),
            )

    private fun exit(direction: Int): ExitTransition =
        slideOutHorizontally(tween(DURATION_MILLIS, easing = FastOutSlowInEasing)) { direction * it / SLIDE_DIVISOR } +
            fadeOut(tween(FADE_OUT_MILLIS))
}
