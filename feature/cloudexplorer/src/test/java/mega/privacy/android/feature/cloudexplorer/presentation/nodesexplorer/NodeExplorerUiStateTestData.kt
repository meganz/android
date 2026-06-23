package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.model.NodeViewItem

/** Builds a [NodeExplorerUiState.Data] with sensible defaults for tests. */
internal fun nodeExplorerDataState(
    currentFolderId: NodeId = NodeId(-1),
    nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    items: List<NodeViewItem<TypedNode>> = emptyList(),
    nodesLoadingState: NodesLoadingState = NodesLoadingState.FullyLoaded,
    searchItems: List<NodeViewItem<TypedNode>> = emptyList(),
    searchLoadingState: NodesLoadingState = NodesLoadingState.FullyLoaded,
    searchedQuery: String? = null,
    showHiddenNodes: Boolean = false,
    isHiddenNodesEnabled: Boolean = false,
    isStorageOverQuota: Boolean = false,
    isConnected: Boolean = true,
    navigateBack: StateEvent = consumed,
    noConnectionEvent: StateEvent = consumed,
    folderName: LocalizedText = LocalizedText.Literal(""),
    isRoot: Boolean = true,
) = NodeExplorerUiState.Data(
    currentFolderId = currentFolderId,
    nodeSourceType = nodeSourceType,
    items = items,
    nodesLoadingState = nodesLoadingState,
    searchItems = searchItems,
    searchLoadingState = searchLoadingState,
    searchedQuery = searchedQuery,
    showHiddenNodes = showHiddenNodes,
    isHiddenNodesEnabled = isHiddenNodesEnabled,
    isStorageOverQuota = isStorageOverQuota,
    isConnected = isConnected,
    navigateBack = navigateBack,
    noConnectionEvent = noConnectionEvent,
    folderName = folderName,
    isRoot = isRoot,
)
