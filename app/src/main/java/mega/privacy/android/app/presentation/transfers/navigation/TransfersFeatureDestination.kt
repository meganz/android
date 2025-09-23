package mega.privacy.android.app.presentation.transfers.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.app.presentation.transfers.view.navigation.transfersScreen
import mega.privacy.android.feature.payment.UpgradeAccount
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.LegacySettings

class TransfersFeatureDestination : FeatureDestination {
    override val navigationGraph: NavGraphBuilder.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            transfersScreen(
                onBackPress = navigationHandler::back,
                onNavigateToStorageSettings = { navigationHandler.navigate(LegacySettings(null)) },
                onNavigateToUpgradeAccount = { navigationHandler.navigate(UpgradeAccount()) },
            )
        }

}