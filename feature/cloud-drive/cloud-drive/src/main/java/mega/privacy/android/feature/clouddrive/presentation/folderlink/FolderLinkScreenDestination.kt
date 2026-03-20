package mega.privacy.android.feature.clouddrive.presentation.folderlink

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.FolderLinkNavKey

fun EntryProviderScope<NavKey>.folderLinkScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<FolderLinkNavKey> { key ->
        val viewModel = hiltViewModel<FolderLinkViewModel, FolderLinkViewModel.Factory> { factory ->
            factory.create(FolderLinkViewModel.Args(uriString = key.uriString))
        }
        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(NodeSourceType.FOLDER_LINK) }
            )
        FolderLinkScreen(
            viewModel = viewModel,
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
            onBack = navigationHandler::back,
            onNavigate = navigationHandler::navigate,
            onTransfer = transferHandler::setTransferEvent,
        )
        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = transferHandler::setTransferEvent,
            nodeResultFlow = navigationHandler::monitorResult,
            clearResultFlow = navigationHandler::clearResult,
        )
    }
}
