package mega.privacy.mobile.home.presentation.home.widget.viewedlinks.mapper

import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.viewedlinks.ViewedLinksSortField
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import javax.inject.Inject

/**
 * Maps between UI-layer [NodeSortOption]/[NodeSortConfiguration] and domain-layer
 * [ViewedLinksSortField]/[SortDirection].
 */

internal class ViewedLinksSortMapper @Inject constructor() {

    /**
     * Maps a UI sort option to the corresponding domain sort field.
     */
    operator fun invoke(sortOption: NodeSortOption): ViewedLinksSortField =
        when (sortOption) {
            NodeSortOption.Name -> ViewedLinksSortField.Name
            else -> ViewedLinksSortField.LastAccessed
        }

    /**
     * Maps a domain sort field and direction to a UI [NodeSortConfiguration].
     */
    operator fun invoke(
        sortField: ViewedLinksSortField,
        sortDirection: SortDirection,
    ): NodeSortConfiguration = NodeSortConfiguration(
        sortOption = when (sortField) {
            ViewedLinksSortField.Name -> NodeSortOption.Name
            ViewedLinksSortField.LastAccessed -> NodeSortOption.LastAccessed
        },
        sortDirection = sortDirection,
    )
}
