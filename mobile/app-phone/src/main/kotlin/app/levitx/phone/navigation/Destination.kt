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
    /** Choose what to work toward. */
    @Serializable
    data object GoalPicker : Destination

    /** Short test of each gate on the active goal, to find the starting point. */
    @Serializable
    data object StartingTest : Destination

    /** The person's path and today's training. */
    @Serializable
    data object Home : Destination

    /** A live session: sets, reps in reserve and rest. */
    @Serializable
    data object Session : Destination

    /** Every gate on the path, with bests. */
    @Serializable
    data object Path : Destination

    /** Bodyweight and bands. */
    @Serializable
    data object Equipment : Destination
}
