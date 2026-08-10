package mega.privacy.android.feature.photos.model

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.shared.nodes.model.SortOptionItem
import mega.privacy.android.shared.resources.R as sharedR

enum class AlbumSortOption(
    override val displayName: Int,
    override val testTag: String,
    override val defaultSortDirection: SortDirection,
) : SortOptionItem {
    Modified(
        sharedR.string.action_sort_by_modified,
        "album_sort_option:sort_modified",
        defaultSortDirection = SortDirection.Descending,
    )
}

@Stable
data class AlbumSortConfiguration(
    val sortOption: AlbumSortOption,
    val sortDirection: SortDirection
)