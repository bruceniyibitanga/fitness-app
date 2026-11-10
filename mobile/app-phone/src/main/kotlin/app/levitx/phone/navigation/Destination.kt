package app.levitx.phone.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Every screen the app can navigate to. Each destination is a key on the
 * back stack.
 *
 * Keys are @Serializable so the back stack can be saved and restored after
 * the system kills the app in the background. Destinations that need input
 * carry it as properties (an id, never a whole object, and never health data,
 * because saved state is written to disk).
 */
sealed interface Destination : NavKey {
    /** The home screen: the person's path and their current gate. */
    @Serializable
    data object Home : Destination
}
