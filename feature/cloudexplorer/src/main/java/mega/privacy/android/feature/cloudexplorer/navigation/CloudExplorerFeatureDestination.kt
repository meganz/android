package mega.privacy.android.feature.cloudexplorer.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.files.ShareFilesToMegaScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.files.ShareFilesToMegaViewModel
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.NodesExplorerNavKey
import mega.privacy.android.navigation.destination.ShareFilesToMegaNavKey

class CloudExplorerFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            shareFilesToMegaDestination(
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

    fun EntryProviderScope<NavKey>.shareFilesToMegaDestination(
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (NavKey) -> Unit,
        onStartUpload: (TransferTriggerEvent) -> Unit,
    ) {
        entry<ShareFilesToMegaNavKey> { key ->
            val viewModel =
                hiltViewModel<ShareFilesToMegaViewModel, ShareFilesToMegaViewModel.Factory> { factory ->
                    factory.create(ShareFilesToMegaViewModel.Args(shareUris = key.shareUris))
                }
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            ShareFilesToMegaScreen(
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
