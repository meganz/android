package mega.privacy.android.app.appstate.content.destinations

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.appstate.content.navigation.view.HomeScreens
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

@Serializable
data class HomeScreensNavKey(
    val initialDestination: NavKey?,
) : NavKey

fun EntryProviderScope<NavKey>.homeScreens(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<HomeScreensNavKey> {
        HomeScreens(
            transferHandler = transferHandler,
            navigationHandler = navigationHandler,
            initialDestination = it.initialDestination,
        )
    }
}