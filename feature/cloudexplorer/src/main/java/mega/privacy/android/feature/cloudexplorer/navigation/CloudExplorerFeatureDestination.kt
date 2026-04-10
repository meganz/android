package mega.privacy.android.feature.cloudexplorer.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.ShareToMegaScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.ShareToMegaViewModel
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.NodesExplorerNavKey
import mega.privacy.android.navigation.destination.ShareToMegaNavKey

class CloudExplorerFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            shareToMegaDestination(
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onStartUpload = transferHandler::setTransferEvent
            )
            nodeExplorerDestination(
                onCloseExplorerScreen = { navigationHandler.backTo(it, true) },
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onStartUpload = transferHandler::setTransferEvent,
            )
        }

    fun EntryProviderScope<NavKey>.shareToMegaDestination(
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (NavKey) -> Unit,
        onStartUpload: (TransferTriggerEvent) -> Unit,
    ) {
        entry<ShareToMegaNavKey> { key ->
            val viewModel =
                hiltViewModel<ShareToMegaViewModel, ShareToMegaViewModel.Factory> { factory ->
                    factory.create(ShareToMegaViewModel.Args(shareUris = key.shareUris))
                }
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            ShareToMegaScreen(
                uiState = uiState,
                startNavKey = key,
                onNavigateBack = { onNavigateBack(key) },
                onStartUpload = onStartUpload,
                onNavigate = onNavigate,
            )
        }
    }

    fun EntryProviderScope<NavKey>.nodeExplorerDestination(
        onCloseExplorerScreen: (NavKey) -> Unit,
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (NavKey) -> Unit,
        onStartUpload: (TransferTriggerEvent) -> Unit,
    ) {
        entry<NodesExplorerNavKey> { key ->
            NodesExplorerScreen(
                explorerMode = key.explorerMode,
                startNavKey = key.startNavKey,
                nodeExplorerId = key.nodeId,
                nodeSourceType = key.nodeSourceType,
                shareUris = key.shareUris,
                onCloseExplorerScreen = { onCloseExplorerScreen(key.startNavKey) },
                onNavigateBack = { onNavigateBack(key) },
                onNavigate = { onNavigate(it) },
                onStartUpload = onStartUpload,
            )
        }
    }
}
