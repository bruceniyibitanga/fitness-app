package app.levitx.wear.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import app.levitx.wear.home.HomeRoute
import kotlinx.serialization.Serializable

/** The watch home screen: the next gate at a glance. */
@Serializable
data object HomeKey : NavKey

/**
 * The watch app's navigation, built on Navigation 3 as the Wear OS docs
 * recommend.
 *
 * The swipe-to-dismiss scene strategy gives every screen Wear's standard
 * gesture: swipe right to go back, with the system's back animation.
 */
@Composable
fun LevitxWearNavDisplay() {
    val backStack = rememberNavBackStack(HomeKey)
    val swipeToDismiss = rememberSwipeDismissableSceneStrategy<NavKey>()
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategies = listOf(swipeToDismiss),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeKey> { HomeRoute() }
        },
    )
}
