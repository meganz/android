package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.appstate.content.destinations.fetchingContentDestination
import mega.privacy.android.app.appstate.content.destinations.homeScreens
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class MainNavigationFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            homeScreens(navigationHandler = navigationHandler, transferHandler = transferHandler)
            fetchingContentDestination()
        }
}