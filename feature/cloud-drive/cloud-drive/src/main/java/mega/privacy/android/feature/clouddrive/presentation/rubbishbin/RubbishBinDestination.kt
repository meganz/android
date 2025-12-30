package mega.privacy.android.feature.clouddrive.presentation.rubbishbin

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.RubbishBinNavKey
import mega.privacy.android.navigation.destination.SearchNodeNavKey

fun EntryProviderScope<NavKey>.rubbishBin(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<RubbishBinNavKey> { key ->
        val viewModel = hiltViewModel<NewRubbishBinViewModel, NewRubbishBinViewModel.Factory>(
            creationCallback = { factory ->
                factory.create(key)
            }
        )
        val nodeOptionsActionViewModel = hiltViewModel<NodeOptionsActionViewModel>()

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = transferHandler::setTransferEvent,
            nodeResultFlow = navigationHandler::monitorResult,
            clearResultFlow = navigationHandler::clearResult,
        )

        RubbishBinScreen(
            viewModel = viewModel,
            navigationHandler = navigationHandler,
            onTransfer = transferHandler::setTransferEvent,
            onFolderClick = {
                navigationHandler.navigate(RubbishBinNavKey(it.longValue))
            },
            openSearch = { parentHandle ->
                navigationHandler.navigate(
                    SearchNodeNavKey(
                        nodeSourceType = NodeSourceType.RUBBISH_BIN,
                        parentHandle = parentHandle
                    )
                )
            },
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
        )
    }
}