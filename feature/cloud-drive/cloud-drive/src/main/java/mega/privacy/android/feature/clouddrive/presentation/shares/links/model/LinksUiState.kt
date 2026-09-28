package mega.privacy.android.feature.clouddrive.presentation.shares.links.model

import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeViewItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * UI state for Links
 */
data class LinksUiState(
    val isLoading: Boolean = true,
    val items: List<NodeViewItem<TypedNode>> = emptyList(),
    val currentViewType: ViewType = ViewType.LIST,
    val selectedSortOrder: SortOrder = SortOrder.ORDER_DEFAULT_ASC,
    val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
) {

    /**
     * True if there are no visible items and not loading
     */
    val isEmpty = items.isEmpty() && !isLoading
}
