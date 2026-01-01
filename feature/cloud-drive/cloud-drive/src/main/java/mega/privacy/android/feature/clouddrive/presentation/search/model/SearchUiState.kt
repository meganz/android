package mega.privacy.android.feature.clouddrive.presentation.search.model

import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState

/**
 * UI state for search screen
 * @param searchText Current text in the search field
 * @param searchedQuery The last searched query
 */
data class SearchUiState(
    val searchText: String = "",
    val searchedQuery: String = "",
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    val items: List<NodeUiItem<TypedNode>> = emptyList(),
    val currentViewType: ViewType = ViewType.LIST,
    val nodesLoadingState: NodesLoadingState = NodesLoadingState.Idle,
    val isHiddenNodeSettingsLoading: Boolean = false, // TODO Change to true when implementing
    val isHiddenNodesEnabled: Boolean = false,
    val showHiddenNodes: Boolean = false,
    val isSelecting: Boolean = false,
    val isContactVerificationOn: Boolean = false,
    val showContactNotVerifiedBanner: Boolean = false,
) {
    /**
     * True if nodes or hidden node settings are loading
     */
    val isLoading = nodesLoadingState == NodesLoadingState.Loading || isHiddenNodeSettingsLoading

    /**
     * Count of visible items based on hidden nodes settings
     */
    val visibleItemsCount: Int

    /**
     * Count of visible selected items
     */
    val selectedItemsCount: Int

    /**
     * True is all nodes are selected
     */
    val isAllSelected: Boolean

    init {
        // Count visible and selected items based on hidden nodes settings with single loop
        if (showHiddenNodes || !isHiddenNodesEnabled) {
            visibleItemsCount = items.size
            selectedItemsCount = items.count { it.isSelected }
        } else {
            var visible = 0
            var selected = 0
            items.forEach { item ->
                if (!item.isSensitive) {
                    visible++
                    if (item.isSelected) {
                        selected++
                    }
                }
            }
            visibleItemsCount = visible
            selectedItemsCount = selected
        }
        isAllSelected = visibleItemsCount == selectedItemsCount
    }

    /**
     * True if any item is selected
     */
    val isInSelectionMode = selectedItemsCount > 0

    /**
     * True if there are no visible items and not loading
     */
    val isEmpty = visibleItemsCount == 0 && !isLoading && searchedQuery.isNotEmpty()

    /**
     * True when no search has been performed yet
     */
    val isPreSearch =
        nodesLoadingState == NodesLoadingState.Idle && (searchText.isEmpty() || searchedQuery.isEmpty())

    /**
     * Returns a list of selected nodes.
     */
    val selectedNodes: List<TypedNode>
        get() = items.mapNotNull { if (it.isSelected) it.node else null }
}