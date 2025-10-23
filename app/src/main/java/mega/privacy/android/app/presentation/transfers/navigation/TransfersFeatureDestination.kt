package mega.privacy.android.app.presentation.transfers.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.transfers.view.navigation.transfersScreen3
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey

class TransfersFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            transfersScreen3(
                onBackPress = navigationHandler::back,
                onNavigateToUpgradeAccount = { navigationHandler.navigate(UpgradeAccountNavKey()) },
            )
        }

}
