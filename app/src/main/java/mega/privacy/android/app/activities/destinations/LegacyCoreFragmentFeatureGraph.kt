package mega.privacy.android.app.activities.destinations

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class LegacyCoreFragmentFeatureGraph : FeatureDestination {
    override val navigationGraph: NavGraphBuilder.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            legacyMediaDiscoveryScreen(navigationHandler)
            notifications(navigationHandler)
        }
}
