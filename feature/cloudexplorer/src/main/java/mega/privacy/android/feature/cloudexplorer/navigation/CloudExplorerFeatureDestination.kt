package mega.privacy.android.feature.cloudexplorer.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerViewModel
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.ChatExplorerNavKey
import mega.privacy.android.navigation.destination.NodesExplorerNavKey

class CloudExplorerFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            nodeExplorerDestination {
                navigationHandler.returnResult(
                    key = NodesExplorerNavKey.SELECTED_ID,
                    value = it
                )
            }
            chatExplorerDestination()
        }

    fun EntryProviderScope<NavKey>.nodeExplorerDestination(
        onFolderSelected: (NodeId) -> Unit,
    ) {
        entry<NodesExplorerNavKey> { key ->
            val viewModel = hiltViewModel<NodesExplorerViewModel>()
            NodesExplorerScreen(
                viewModel = viewModel,
                onFolderDestinationSelected = { onFolderSelected(it.id) }
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
