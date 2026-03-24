package mega.privacy.android.feature.cloudexplorer.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerSharedViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.ShareToMegaScreen
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
            ShareToMegaScreen()
        }
    }

    fun EntryProviderScope<NavKey>.nodeExplorerDestination(
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (NavKey) -> Unit,
    ) {
        entry<NodesExplorerNavKey> { key ->
            val viewModel =
                hiltViewModel<NodesExplorerViewModel, NodesExplorerViewModel.Factory> { factory ->
                    factory.create(
                        args = NodeExplorerSharedViewModel.Args(
                            key.nodeId,
                            key.nodeSourceType
                        )
                    )
                }

            NodesExplorerScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBack(key) },
                onNavigateToFolder = {
                    onNavigate(
                        NodesExplorerNavKey(
                            nodeId = it,
                            nodeSourceType = key.nodeSourceType,
                        )
                    )
                },
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
