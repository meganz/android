package mega.privacy.android.feature.clouddrive.presentation.shares

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.SharesNavKey

fun EntryProviderScope<NavKey>.shares(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<SharesNavKey> {
        val nodeOptionsActionViewModel = hiltViewModel<NodeOptionsActionViewModel>()

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = transferHandler::setTransferEvent,
            nodeResultFlow = navigationHandler::monitorResult,
            clearResultFlow = navigationHandler::clearResult,
        )

        SharesScreen(
            navigationHandler = navigationHandler,
            onTransfer = transferHandler::setTransferEvent,
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
        )
    }
}