package mega.privacy.android.core.nodecomponents.mapper

import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.SortDirection
import javax.inject.Inject

class NodeSortConfigurationUiMapper @Inject constructor() {
    operator fun invoke(order: SortOrder): NodeSortConfiguration =
        when (order) {
            SortOrder.ORDER_DEFAULT_ASC -> NodeSortConfiguration(
                NodeSortOption.Name,
                SortDirection.Ascending
            )

            SortOrder.ORDER_DEFAULT_DESC -> NodeSortConfiguration(
                NodeSortOption.Name,
                SortDirection.Descending
            )

            SortOrder.ORDER_FAV_ASC -> NodeSortConfiguration(
                NodeSortOption.Favourite,
                SortDirection.Ascending
            )

            SortOrder.ORDER_FAV_DESC -> NodeSortConfiguration(
                NodeSortOption.Favourite,
                SortDirection.Descending
            )

            SortOrder.ORDER_LABEL_ASC -> NodeSortConfiguration(
                NodeSortOption.Label,
                SortDirection.Ascending
            )

            SortOrder.ORDER_LABEL_DESC -> NodeSortConfiguration(
                NodeSortOption.Label,
                SortDirection.Descending
            )

            SortOrder.ORDER_CREATION_ASC -> NodeSortConfiguration(
                NodeSortOption.Created,
                SortDirection.Ascending
            )

            SortOrder.ORDER_CREATION_DESC -> NodeSortConfiguration(
                NodeSortOption.Created,
                SortDirection.Descending
            )

            SortOrder.ORDER_MODIFICATION_ASC -> NodeSortConfiguration(
                NodeSortOption.Modified,
                SortDirection.Ascending
            )

            SortOrder.ORDER_MODIFICATION_DESC -> NodeSortConfiguration(
                NodeSortOption.Modified,
                SortDirection.Descending
            )

            SortOrder.ORDER_SIZE_ASC -> NodeSortConfiguration(
                NodeSortOption.Size,
                SortDirection.Ascending
            )

            SortOrder.ORDER_SIZE_DESC -> NodeSortConfiguration(
                NodeSortOption.Size,
                SortDirection.Descending
            )

            else -> NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending)
        }

    operator fun invoke(config: NodeSortConfiguration): SortOrder {
        val isAscending = config.sortDirection == SortDirection.Ascending

        return when (config.sortOption) {
            NodeSortOption.Name -> if (isAscending) SortOrder.ORDER_DEFAULT_ASC else SortOrder.ORDER_DEFAULT_DESC
            NodeSortOption.Favourite -> if (isAscending) SortOrder.ORDER_FAV_ASC else SortOrder.ORDER_FAV_DESC
            NodeSortOption.Label -> if (isAscending) SortOrder.ORDER_LABEL_ASC else SortOrder.ORDER_LABEL_DESC
            NodeSortOption.Created -> if (isAscending) SortOrder.ORDER_CREATION_ASC else SortOrder.ORDER_CREATION_DESC
            NodeSortOption.Modified -> if (isAscending) SortOrder.ORDER_MODIFICATION_ASC else SortOrder.ORDER_MODIFICATION_DESC
            NodeSortOption.Size -> if (isAscending) SortOrder.ORDER_SIZE_ASC else SortOrder.ORDER_SIZE_DESC
        }
    }
}