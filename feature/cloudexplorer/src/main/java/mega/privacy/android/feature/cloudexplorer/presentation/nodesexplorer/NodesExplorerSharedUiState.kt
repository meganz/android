package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeUiItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * UI state for [NodeExplorerSharedViewModel].
 */
data class NodesExplorerSharedUiState(
    val nodesLoadingState: NodesLoadingState = NodesLoadingState.Loading,
    val items: List<NodeUiItem<TypedNode>> = emptyList(),
    val isHiddenNodeSettingsLoading: Boolean = true,
    val showHiddenNodes: Boolean = false,
    val isHiddenNodesEnabled: Boolean = false,
    val viewType: ViewType = ViewType.LIST,
    val navigateBack: StateEvent = consumed,
    val sortOrder: SortOrder = SortOrder.ORDER_DEFAULT_ASC,
    val nodeSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.Companion.default,
    val isStorageOverQuota: Boolean = false,
    val isSelectionModeEnabled: Boolean = false,
) {
    val isLoading = nodesLoadingState == NodesLoadingState.Loading || isHiddenNodeSettingsLoading
}