package mega.privacy.android.core.nodecomponents.model

import androidx.annotation.StringRes
import mega.privacy.android.core.nodecomponents.sheet.sort.SortOptionItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.shared.resources.R as sharedR

enum class NodeSortOption(
    @StringRes override val displayName: Int,
    override val testTag: String = "$displayName",
) : SortOptionItem {
    Name(sharedR.string.action_sort_by_name),
    Favourite(sharedR.string.action_sort_by_favorite),
    Label(sharedR.string.action_sort_by_label),
    Created(sharedR.string.search_dropdown_chip_filter_type_date_added),
    Modified(sharedR.string.search_dropdown_chip_filter_type_last_modified),
    Size(sharedR.string.action_sort_by_size),
    ShareCreated(sharedR.string.action_sort_by_share_created),
    LinkCreated(sharedR.string.action_sort_by_link_created);

    /**
     * Get available sort options based on the node source type
     */
    companion object {
        fun getOptionsForSourceType(sourceType: NodeSourceType) = when (sourceType) {
            NodeSourceType.INCOMING_SHARES -> listOf(
                Name,
                Created,
                Modified,
                Size
            )

            NodeSourceType.OUTGOING_SHARES -> listOf(
                Name,
                Favourite,
                Label,
                ShareCreated,
                Modified,
                Size
            )

            NodeSourceType.LINKS -> listOf(
                Name,
                Favourite,
                Label,
                LinkCreated,
                Modified,
                Size
            )

            NodeSourceType.OFFLINE -> listOf(
                Name,
                Size,
                Modified
            )

            NodeSourceType.CLOUD_DRIVE -> listOf(
                Name,
                Favourite,
                Label,
                Created,
                Modified,
                Size
            )

            NodeSourceType.RUBBISH_BIN -> listOf(
                Name,
                Favourite,
                Label,
                Created,
                Modified,
                Size
            )

            else -> listOf(
                Name,
                Favourite,
                Label,
                Created,
                Modified,
                Size
            )
        }
    }
}

data class NodeSortConfiguration(
    val sortOption: NodeSortOption,
    val sortDirection: SortDirection,
) {
    companion object {
        val default = NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending)
    }
}