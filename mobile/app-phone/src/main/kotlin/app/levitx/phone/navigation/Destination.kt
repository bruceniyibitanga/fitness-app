package app.levitx.phone.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Every screen the app can navigate to. Each destination is a key on the
 * back stack.
 *
 * Keys are @Serializable so the back stack can be saved and restored after
 * the system kills the app in the background. They never carry health data,
 * because saved state is written to disk. Screens read what they need from
 * the encrypted database instead.
 */
sealed interface Destination : NavKey {
    /** Setup: the goal, how to train and the week, then the first plan. */
    @Serializable
    data object Onboarding : Destination

    /** The tabs: Today, Train, Plan, Skills and Body. */
    @Serializable
    data object Main : Destination

    /** The workout in progress. It is read from the saved draft, so this key needs nothing else. */
    @Serializable
    data object Workout : Destination
}
