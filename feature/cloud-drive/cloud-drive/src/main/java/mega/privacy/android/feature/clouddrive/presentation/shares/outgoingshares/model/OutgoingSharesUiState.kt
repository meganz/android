package mega.privacy.android.feature.clouddrive.presentation.shares.outgoingshares.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * UI state for Outgoing Shares
 *
 * @property currentFolderId The current folder id being displayed
 * @property items List of nodes in the current folder
 * @property currentViewType The current view type of the Cloud Drive
 * @property navigateToFolderEvent Event to navigate to a folder
 * @property navigateBack Event to navigate back
 * @property isSelecting True if nodes are being selected
 * @property hasMediaItems True if there are media(image, video) items in the current folder
 */
data class OutgoingSharesUiState(
    val isLoading: Boolean = true,
    val currentFolderId: NodeId = NodeId(-1L),
    val items: List<NodeUiItem<TypedNode>> = emptyList(),
    val currentViewType: ViewType = ViewType.LIST,
    val navigateToFolderEvent: StateEventWithContent<TypedNode> = consumed(),
    val navigateBack: StateEvent = consumed,
    val isSelecting: Boolean = false,
    val hasMediaItems: Boolean = false,
    val selectedSortOrder: SortOrder = SortOrder.ORDER_DEFAULT_ASC,
    val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
) {

    /**
     * Count of visible selected items
     */
    val selectedItemsCount: Int = items.count { it.isSelected }

    /**
     * True if any item is selected
     */
    val isInSelectionMode = selectedItemsCount > 0

    /**
     * True if there are no visible items and not loading
     */
    val isEmpty = items.isEmpty() && !isLoading

    /**
     * Returns a list of selected nodes.
     */
    val selectedNodes: List<TypedNode>
        get() = items.mapNotNull { if (it.isSelected) it.node else null }

    /**
     * Returns a list of selected node ids.
     */
    val selectedNodeIds: List<NodeId>
        get() = selectedNodes.map { it.id }
}