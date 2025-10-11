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
    Created(sharedR.string.action_sort_by_created),
    Modified(sharedR.string.action_sort_by_modified),
    Size(sharedR.string.action_sort_by_size);

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