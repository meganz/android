package mega.privacy.mobile.home.presentation.recents.bucket

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberNodeActionHandler
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
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
        val nodeOptionsActionViewModel = hiltViewModel<NodeOptionsActionViewModel>()
        val nodeActionHandler = rememberNodeActionHandler(
            navigationHandler = navigationHandler,
            viewModel = nodeOptionsActionViewModel,
            megaNavigator = rememberMegaNavigator(),
        )

        RecentsBucketScreen(
            viewModel = viewModel,
            onNavigate = navigationHandler::navigate,
            transferHandler = transferHandler,
            nodeActionHandler = nodeActionHandler,
            onBack = navigationHandler::back,
            nodeSourceType = navKey.nodeSourceType,
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

