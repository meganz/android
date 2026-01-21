package mega.privacy.mobile.home.presentation.recents.bucket

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.RecentsBucketScreenNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator

fun EntryProviderScope<NavKey>.recentsBucketScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<RecentsBucketScreenNavKey> { navKey ->
        val viewModel =
            hiltViewModel<RecentsBucketViewModel, RecentsBucketViewModel.Factory> { factory ->
                factory.create(
                    RecentsBucketViewModel.Args(
                        identifier = navKey.identifier,
                        isMediaBucket = navKey.isMediaBucket,
                        folderName = navKey.folderName,
                        folderHandle = navKey.folderHandle,
                        nodeSourceType = navKey.nodeSourceType,
                        timestamp = navKey.timestamp,
                        fileCount = navKey.fileCount,
                    )
                )
            }
        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(NodeSourceType.RECENTS_BUCKET) }
            )
        val selectionModeActionHandler = rememberMultiNodeActionHandler(
            navigationHandler = navigationHandler,
            viewModel = nodeOptionsActionViewModel,
            megaNavigator = rememberMegaNavigator(),
        )

        RecentsBucketScreen(
            viewModel = viewModel,
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            transferHandler = transferHandler,
            onBack = navigationHandler::back,
            nodeSourceType = navKey.nodeSourceType,
            selectionModeActionHandler = selectionModeActionHandler
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

