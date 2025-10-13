package mega.privacy.mobile.home.navigation

import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.mobile.home.presentation.configuration.homeConfigurationScreen

class HomeFeatureGraph : FeatureDestination {
    override val navigationGraph: EntryProviderBuilder<NavKey>.(NavigationHandler, TransferHandler) -> Unit
        get() = { navigationHandler, transferHandler ->
            homeConfigurationScreen(navigationHandler = navigationHandler)
        }
}