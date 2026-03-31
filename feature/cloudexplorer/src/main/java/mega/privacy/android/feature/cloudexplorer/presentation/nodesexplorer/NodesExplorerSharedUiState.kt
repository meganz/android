package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.model.NodeViewItem

/**
 * UI state for [NodeExplorerSharedViewModel].
 */
data class NodesExplorerSharedUiState(
    val currentFolderId: NodeId = NodeId(-1),
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    val nodesLoadingState: NodesLoadingState = NodesLoadingState.Loading,
    val items: List<NodeViewItem<TypedNode>> = emptyList(),
    val isHiddenNodeSettingsLoading: Boolean = true,
    val showHiddenNodes: Boolean = false,
    val isHiddenNodesEnabled: Boolean = false,
    val navigateBack: StateEvent = consumed,
    val isStorageOverQuota: Boolean = false,
    val isSelectionModeEnabled: Boolean = false,
) {
    val isLoading = nodesLoadingState == NodesLoadingState.Loading || isHiddenNodeSettingsLoading
}