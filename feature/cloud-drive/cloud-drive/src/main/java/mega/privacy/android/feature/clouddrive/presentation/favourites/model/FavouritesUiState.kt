package mega.privacy.android.feature.clouddrive.presentation.favourites.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * UI state for Favourites
 * @property isLoading True if nodes are currently loading
 * @property items List of favourite nodes
 * @property currentViewType The current view type
 * @property navigateToFolderEvent Event to navigate to a folder
 * @property openedFileNode The file node that is currently opened
 */
data class FavouritesUiState(
    val isLoading: Boolean = true,
    val items: List<NodeUiItem<TypedNode>> = emptyList(),
    val currentViewType: ViewType = ViewType.LIST,
    val navigateToFolderEvent: StateEventWithContent<TypedNode> = consumed(),
    val openedFileNode: TypedFileNode? = null,
    val selectedSortOrder: SortOrder = SortOrder.ORDER_DEFAULT_ASC,
    val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
) {

    /**
     * Count of visible items
     */
    val visibleItemsCount: Int = items.size

    /**
     * Count of visible selected items
     */
    val selectedItemsCount: Int = items.count { it.isSelected }

    /**
     * True is all nodes are selected
     */
    val isAllSelected: Boolean = visibleItemsCount == selectedItemsCount && visibleItemsCount > 0

    /**
     * True if any item is selected
     */
    val isInSelectionMode = selectedItemsCount > 0

    /**
     * True if there are no visible items and not loading
     */
    val isEmpty = visibleItemsCount == 0 && !isLoading

    /**
     * Returns a list of selected nodes.
     */
    val selectedNodes: List<TypedNode>
        get() = items.mapNotNull { if (it.isSelected) it.node else null }
}
