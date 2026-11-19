package app.levitx.phone.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation3.ui.LocalNavAnimatedContentScope

/*
 * Photos, and how one travels from a card to the page the card opens.
 */

/**
 * The scope shared photos move in, provided once around the app's navigation.
 * Null where there is no navigation, such as a preview: photos just draw.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/**
 * A photo that fills its space. When the screen being opened shows a photo
 * with the same [sharedKey], the photo grows from where it was into its new
 * place, so a card visibly becomes the page it opens. With no key, or no
 * match on the other screen, it is an ordinary photo.
 *
 * It is drawn inside its own screen, not above everything, so the fade and
 * the title over it stay in front during the move.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Photo(@DrawableRes photo: Int, modifier: Modifier = Modifier, sharedKey: String? = null) {
    val shared = LocalSharedTransitionScope.current
    val placed = if (shared == null || sharedKey == null) {
        modifier
    } else {
        val screen = LocalNavAnimatedContentScope.current
        with(shared) {
            modifier.sharedElement(
                rememberSharedContentState(sharedKey),
                animatedVisibilityScope = screen,
                renderInOverlayDuringTransition = false,
            )
        }
    }
    // Photos are decoration: what they show is already said by the title over them.
    Image(painterResource(photo), contentDescription = null, contentScale = ContentScale.Crop, modifier = placed)
}

/** The key a skill program's photo shares between its card and its page. */
fun programPhotoKey(pathId: String): String = "photo/program/$pathId"

/** The key a single workout's photo shares between its tile and its page, by the focus's name. */
fun focusPhotoKey(focusName: String): String = "photo/focus/$focusName"
