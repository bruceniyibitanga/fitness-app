package app.levitx.phone.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import app.levitx.phone.home.HomeRoute
import kotlinx.serialization.Serializable

/** The home screen: the person's path and their current gate. */
@Serializable
data object HomeKey : NavKey

/**
 * The phone app's navigation, built on Navigation 3.
 *
 * The back stack is a list of [NavKey]s that we own, so moving between screens
 * is adding to or removing from a list. It is saved across configuration
 * changes and process death.
 *
 * The decorators give each screen its own saved state and its own ViewModel
 * store, so a ViewModel lives exactly as long as its screen is on the stack.
 */
@Composable
fun LevitxNavDisplay(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(HomeKey)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeKey> { HomeRoute() }
        },
    )
}
