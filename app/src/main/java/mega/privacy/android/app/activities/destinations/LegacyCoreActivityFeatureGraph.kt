package mega.privacy.android.app.activities.destinations

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.feature.payment.upgradeAccount
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class LegacyCoreActivityFeatureGraph : FeatureDestination {
    override val navigationGraph: NavGraphBuilder.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            overDiskQuotaPaywallWarning(navigationHandler::back)
            upgradeAccount(navigationHandler::back)
            myAccount(navigationHandler::back)
            webDestinations(navigationHandler::back)
        }
}