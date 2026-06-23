package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEvent
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
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
        val isConnected: Boolean,
        val navigateBack: StateEvent,
        val noConnectionEvent: StateEvent,
        val folderName: LocalizedText,
        val isRoot: Boolean,
    ) : NodeExplorerUiState
}
