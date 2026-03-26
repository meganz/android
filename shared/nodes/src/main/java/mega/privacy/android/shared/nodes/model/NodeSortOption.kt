package mega.privacy.android.shared.nodes.model

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.shared.resources.R as sharedR

enum class NodeSortOption(
    @StringRes override val displayName: Int,
    override val testTag: String = "$displayName",
    override val defaultSortDirection: SortDirection,
) : SortOptionItem {
    Name(sharedR.string.action_sort_by_name, defaultSortDirection = SortDirection.Ascending),

    Favourite(
        sharedR.string.action_sort_by_favorite,
        defaultSortDirection = SortDirection.Descending
    ),

    Label(sharedR.string.action_sort_by_label, defaultSortDirection = SortDirection.Ascending),

    Created(
        sharedR.string.search_dropdown_chip_filter_type_date_added,
        defaultSortDirection = SortDirection.Descending,
    ),

    Modified(
        sharedR.string.search_dropdown_chip_filter_type_last_modified,
        defaultSortDirection = SortDirection.Descending,
    ),

    Size(sharedR.string.action_sort_by_size, defaultSortDirection = SortDirection.Descending),

    ShareCreated(
        sharedR.string.action_sort_by_share_created,
        defaultSortDirection = SortDirection.Descending,
    ),
    LinkCreated(
        sharedR.string.action_sort_by_link_created,
        defaultSortDirection = SortDirection.Descending,
    );

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

            NodeSourceType.VIDEO_PLAYLISTS -> listOf(
                Name,
                Created,
                Modified
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
