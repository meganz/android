package mega.privacy.android.feature.clouddrive.presentation.shares.links.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * UI state for Links
 */
data class LinksUiState(
    val isLoading: Boolean = true,
    val items: List<NodeUiItem<TypedNode>> = emptyList(),
    val currentViewType: ViewType = ViewType.LIST,
    val navigateToFolderEvent: StateEventWithContent<TypedNode> = consumed(),
    val navigateBack: StateEvent = consumed,
    val openedFileNode: TypedFileNode? = null,
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
     * True if all items are selected
     */
    val isAllSelected = selectedItemsCount == items.size

    /**
     * True if there are no visible items and not loading
     */
    val isEmpty = items.isEmpty() && !isLoading

    /**
     * Returns a list of selected nodes.
     */
    val selectedNodes: List<TypedNode>
        get() = items.mapNotNull { item ->
            if (item.isSelected) {
                when (val node = item.node) {
                    is PublicLinkFolder -> node.node
                    is PublicLinkFile -> node.node
                    else -> node
                }
            } else {
                null
            }
        }
}