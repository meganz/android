package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEvent
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.cloudexplorer.presentation.components.selectableNodeIds
import mega.privacy.android.feature.cloudexplorer.presentation.components.visibleNodeItems
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.TabSignal
import mega.privacy.android.shared.nodes.model.NodeViewItem

/**
 * Single UI state for every node explorer source (cloud, incoming shares, favourites). [folderName]
 * and [isRoot] are only meaningful for the cloud-drive source; other sources leave them at their
 * source-level constants.
 */
@Stable
sealed interface NodeExplorerUiState {

    data object Loading : NodeExplorerUiState

    data class Data(
        val currentFolderId: NodeId,
        val nodeSourceType: NodeSourceType,
        val items: List<NodeViewItem<TypedNode>>,
        val nodesLoadingState: NodesLoadingState,
        val searchItems: List<NodeViewItem<TypedNode>>,
        val searchLoadingState: NodesLoadingState,
        val searchedQuery: String?,
        val showHiddenNodes: Boolean,
        val isHiddenNodesEnabled: Boolean,
        val isStorageOverQuota: Boolean,
        val navigateBack: StateEvent,
        val folderName: LocalizedText,
        val isRoot: Boolean,
    ) : NodeExplorerUiState
}

internal fun NodeExplorerUiState.toTabSignal(
    disabledNodeIds: Set<NodeId> = emptySet(),
    videosOnly: Boolean = false,
    isFileSelectionEnabled: Boolean = false,
): TabSignal =
    when (this) {
        NodeExplorerUiState.Loading -> TabSignal(isLoading = true)
        is NodeExplorerUiState.Data -> TabSignal(
            isLoading = false,
            hasContent = items.isNotEmpty(),
            folderName = folderName,
            selectableNodeIds = if (isFileSelectionEnabled) {
                selectableNodeIds(
                    items = visibleNodeItems(items, showHiddenNodes, isHiddenNodesEnabled),
                    disabledNodeIds = disabledNodeIds,
                    videosOnly = videosOnly,
                )
            } else emptySet(),
            nodesLoadingState = nodesLoadingState,
        )
    }

internal data class MappedItems(
    val items: List<NodeViewItem<TypedNode>>,
    val loadingState: NodesLoadingState,
)

internal data class SearchResult(
    val nodes: List<TypedNode>,
    val loadingState: NodesLoadingState,
    val query: String?,
)

internal data class SearchState(
    val items: List<NodeViewItem<TypedNode>>,
    val loadingState: NodesLoadingState,
    val query: String?,
)

internal data class Global(
    val isHiddenNodesEnabled: Boolean,
    val showHiddenNodes: Boolean,
    val isStorageOverQuota: Boolean,
)

internal data class FolderInfo(
    val folderName: LocalizedText,
    val isRoot: Boolean,
)

data class NodesResult(
    val nodes: List<TypedNode>,
    val loadingState: NodesLoadingState,
)
