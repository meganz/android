package mega.privacy.android.feature.cloudexplorer.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.model.ExplorerModeData
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.model.toData
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.ShareToMegaScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.ShareToMegaViewModel
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ChatExplorerNavKey
import mega.privacy.android.navigation.destination.NodesExplorerNavKey
import mega.privacy.android.navigation.destination.ShareToMegaNavKey

class CloudExplorerFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            shareToMegaDestination(
                onNavigateBack = { navigationHandler.remove(it) },
                onNavigate = { navigationHandler.navigate(it) },
            )
            nodeExplorerDestination(
                onNavigateBack = { navigationHandler.remove(it) },
                onNavigate = { navigationHandler.navigate(it) },
            )
            chatExplorerDestination()
        }

    fun EntryProviderScope<NavKey>.shareToMegaDestination(
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (NavKey) -> Unit,
    ) {
        entry<ShareToMegaNavKey>(
            metadata = transparentMetadata()
        ) { key ->
            val viewModel =
                hiltViewModel<ShareToMegaViewModel, ShareToMegaViewModel.Factory> { factory ->
                    factory.create(ShareToMegaViewModel.Args(shareUris = key.shareUris))
                }
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            ShareToMegaScreen(
                uiState = uiState,
                onNavigateBack = { onNavigateBack(key) },
                onUpload = viewModel::upload,
                onNavigateToFolder = onNavigate,
            )
        }
    }

    fun EntryProviderScope<NavKey>.nodeExplorerDestination(
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (NavKey) -> Unit,
    ) {
        entry<NodesExplorerNavKey> { key ->
            NodesExplorerScreen(
                explorerModeData = key.explorerMode.toData(),
                nodeExplorerId = key.nodeId,
                nodeSourceType = key.nodeSourceType,
                onNavigateBack = { onNavigateBack(key) },
                onNavigateToFolder = { onNavigate(it) },
            )
        }
    }

    fun EntryProviderScope<NavKey>.chatExplorerDestination() {
        entry<ChatExplorerNavKey> { key ->
            val viewModel = hiltViewModel<ChatExplorerViewModel>()

            ChatExplorerScreen()
        }
    }
}
