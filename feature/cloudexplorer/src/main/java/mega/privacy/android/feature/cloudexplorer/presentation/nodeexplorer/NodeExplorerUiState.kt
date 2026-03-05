package mega.privacy.android.feature.cloudexplorer.presentation.nodeexplorer

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType

data class NodeExplorerUiState(
    val items: List<NodeUiItem<TypedNode>> = emptyList(),
    val viewType: ViewType = ViewType.LIST,
    val navigateBack: StateEvent = consumed,
    val sortOrder: SortOrder = SortOrder.ORDER_DEFAULT_ASC,
    val nodeSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
    val isStorageOverQuota: Boolean = false,
    val isSelectionModeEnabled: Boolean = false,
)