package mega.privacy.android.core.nodecomponents.model

import androidx.annotation.StringRes
import mega.privacy.android.core.nodecomponents.sheet.sort.SortOptionItem
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.shared.resources.R as sharedR

enum class NodeSortOption(
    @StringRes override val displayName: Int,
    override val testTag: String = "$displayName"
) : SortOptionItem {
    Name(sharedR.string.action_sort_by_name),
    Favourite(sharedR.string.action_sort_by_favorite),
    Label(sharedR.string.action_sort_by_label),
    Created(sharedR.string.action_sort_by_created),
    Modified(sharedR.string.action_sort_by_modified),
    Size(sharedR.string.action_sort_by_size);
}

data class NodeSortConfiguration(
    val sortOption: NodeSortOption,
    val sortDirection: SortDirection
) {
    companion object {
        val default = NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending)
    }
}