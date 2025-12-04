package mega.privacy.android.app.appstate.content.destinations

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import mega.privacy.android.app.appstate.content.navigation.view.HomeScreens
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.HomeScreensNavKey

fun EntryProviderScope<NavKey>.homeScreens(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<HomeScreensNavKey>(
        metadata = NavDisplay.transitionSpec {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) { key ->
        HomeScreens(
            transferHandler = transferHandler,
            outerNavigationHandler = navigationHandler,
            initialDestination = key.root?.let { it to key.destinations },
        )
    }
}
