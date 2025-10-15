package mega.privacy.android.feature.clouddrive.presentation.shares

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.SharesNavKey

fun EntryProviderScope<NavKey>.shares(
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