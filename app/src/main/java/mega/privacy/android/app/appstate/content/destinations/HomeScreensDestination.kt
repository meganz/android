package mega.privacy.android.app.appstate.content.destinations

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.appstate.content.navigation.view.HomeScreens
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.HomeScreensNavKey

fun EntryProviderScope<NavKey>.homeScreens(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<HomeScreensNavKey> { key ->
        HomeScreens(
            transferHandler = transferHandler,
            outerNavigationHandler = navigationHandler,
            initialDestination = key.root?.let { it to key.destinations },
        )
    }
}
