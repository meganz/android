package mega.privacy.android.feature.cloudexplorer.presentation.explorer

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerUiState
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.favouritesexplorer.FavouritesExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.incomingsharesexplorer.IncomingSharesExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerUiState
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.nodeExplorerDataState
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * Wires the explorer ViewModels (cloud + each tab) into a [ViewModelStoreOwner] so the host
 * [ExplorerScreen] resolves them via `hiltViewModel` from the store instead of the Hilt graph.
 */
internal fun explorerViewModelStoreOwner(
    nodes: NodesExplorerViewModel = stubNodesExplorerViewModel(),
    incoming: IncomingSharesExplorerViewModel = stubIncomingSharesExplorerViewModel(),
    favourites: FavouritesExplorerViewModel = stubFavouritesExplorerViewModel(),
    chat: ChatExplorerViewModel = stubChatExplorerViewModel(),
): ViewModelStoreOwner {
    val store = mock<ViewModelStore> {
        on { get(argThat<String> { keyOf(NodesExplorerViewModel::class.java) }) } doReturn nodes
        on { get(argThat<String> { keyOf(IncomingSharesExplorerViewModel::class.java) }) } doReturn incoming
        on { get(argThat<String> { keyOf(FavouritesExplorerViewModel::class.java) }) } doReturn favourites
        on { get(argThat<String> { keyOf(ChatExplorerViewModel::class.java) }) } doReturn chat
    }
    return mock { on { viewModelStore } doReturn store }
}

private fun String.keyOf(type: Class<*>) = contains(type.canonicalName.orEmpty())

internal fun stubNodesExplorerViewModel(): NodesExplorerViewModel = mock {
    on { uiState } doReturn MutableStateFlow<NodeExplorerUiState>(nodeExplorerDataState())
}

internal fun stubIncomingSharesExplorerViewModel(): IncomingSharesExplorerViewModel = mock {
    on { uiState } doReturn MutableStateFlow<NodeExplorerUiState>(nodeExplorerDataState())
}

internal fun stubFavouritesExplorerViewModel(): FavouritesExplorerViewModel = mock {
    on { uiState } doReturn MutableStateFlow<NodeExplorerUiState>(nodeExplorerDataState())
}

internal fun stubChatExplorerViewModel(): ChatExplorerViewModel = mock {
    on { uiState } doReturn MutableStateFlow(ChatExplorerUiState.Loading)
}
