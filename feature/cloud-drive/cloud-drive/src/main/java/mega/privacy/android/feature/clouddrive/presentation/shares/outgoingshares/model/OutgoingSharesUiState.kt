package mega.privacy.android.feature.clouddrive.presentation.shares.outgoingshares.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeViewItem

/**
 * UI state for Outgoing Shares
 *
 * @property isLoading True while the first page of nodes is still being loaded
 * @property currentFolderId The current folder id being displayed
 * @property items List of nodes in the current folder
 * @property currentViewType The current view type of the Cloud Drive
 * @property navigateBack Event to navigate back
 * @property hasMediaItems True if there are media(image, video) items in the current folder
 * @property selectedSortOrder The sort order applied to [items]
 * @property selectedSortConfiguration The sort order as shown in the nodes header
 */
data class OutgoingSharesUiState(
    val isLoading: Boolean = true,
    val currentFolderId: NodeId = NodeId(-1L),
    val items: List<NodeViewItem<TypedNode>> = emptyList(),
    val currentViewType: ViewType = ViewType.LIST,
    val navigateBack: StateEvent = consumed,
    val hasMediaItems: Boolean = false,
    val selectedSortOrder: SortOrder = SortOrder.ORDER_DEFAULT_ASC,
    val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
) {

    /**
     * True if there are no visible items and not loading
     */
    val isEmpty = items.isEmpty() && !isLoading
}
