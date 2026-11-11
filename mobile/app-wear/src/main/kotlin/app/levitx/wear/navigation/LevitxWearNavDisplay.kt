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
import app.levitx.wear.load.LoadRoute
import app.levitx.wear.session.SessionRoute

/**
 * The watch app's navigation, built on Navigation 3 as the Wear OS docs
 * recommend.
 *
 * The swipe-to-dismiss scene strategy gives every screen Wear's standard
 * gesture: swipe right to go back, with the system's back animation.
 */
@Composable
fun LevitxWearNavDisplay() {
    val backStack = rememberNavBackStack(Destination.Home)
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
            entry<Destination.Home> {
                HomeRoute(
                    onOpenSession = { backStack.add(Destination.Session) },
                    onChooseLoad = { backStack.add(Destination.Load) },
                )
            }
            entry<Destination.Session> {
                SessionRoute(
                    onClose = {
                        // Only ever closes the session screen, even if it is asked to twice.
                        if (backStack.lastOrNull() == Destination.Session) backStack.removeLastOrNull()
                    },
                )
            }
            entry<Destination.Load> { LoadRoute() }
        },
    )
}
