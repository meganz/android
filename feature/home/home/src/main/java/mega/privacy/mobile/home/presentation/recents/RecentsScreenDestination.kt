package mega.privacy.mobile.home.presentation.recents

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.RecentsScreenNavKey
import mega.privacy.mobile.home.presentation.recents.RecentsWidgetConstants.SCREEN_MAX_BUCKETS

fun EntryProviderScope<NavKey>.recentsScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<RecentsScreenNavKey> {
        val viewModel =
            hiltViewModel<RecentsViewModel, RecentsViewModel.Factory> { factory ->
                factory.create(maxBucketCount = SCREEN_MAX_BUCKETS)
            }
        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(NodeSourceType.RECENTS_BUCKET) }
            )

        RecentsScreen(
            viewModel = viewModel,
            onNavigate = navigationHandler::navigate,
            transferHandler = transferHandler,
            onBack = navigationHandler::back,
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
