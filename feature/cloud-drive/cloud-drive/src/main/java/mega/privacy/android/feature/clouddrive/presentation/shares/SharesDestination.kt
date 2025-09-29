package mega.privacy.android.feature.clouddrive.presentation.shares

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.SharesNavKey

fun NavGraphBuilder.shares(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    composable<SharesNavKey> {
        SharesScreen(
            navigationHandler = navigationHandler,
            onTransfer = transferHandler::setTransferEvent,
        )
    }
}