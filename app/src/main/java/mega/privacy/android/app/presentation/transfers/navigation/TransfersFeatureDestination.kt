package mega.privacy.android.app.presentation.transfers.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.app.presentation.transfers.view.navigation.transfersScreen
import mega.privacy.android.feature.payment.UpgradeAccountNavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class TransfersFeatureDestination : FeatureDestination {
    override val navigationGraph: NavGraphBuilder.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            transfersScreen(
                onBackPress = navigationHandler::back,
                onNavigateToUpgradeAccount = { navigationHandler.navigate(UpgradeAccountNavKey()) },
            )
        }

}