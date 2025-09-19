package mega.privacy.mobile.home.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.mobile.home.presentation.configuration.homeConfigurationScreen

class HomeFeatureGraph : FeatureDestination {
    override val navigationGraph: NavGraphBuilder.(NavigationHandler, TransferHandler) -> Unit
        get() = { navigationHandler, transferHandler ->
            homeConfigurationScreen(navigationHandler = navigationHandler)
        }
}