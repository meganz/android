package mega.privacy.android.feature.clouddrive.presentation.shares

import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.SharesNavKey

fun EntryProviderBuilder<NavKey>.shares(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<SharesNavKey> {
        SharesScreen(
            navigationHandler = navigationHandler,
            onTransfer = transferHandler::setTransferEvent,
        )
    }
}