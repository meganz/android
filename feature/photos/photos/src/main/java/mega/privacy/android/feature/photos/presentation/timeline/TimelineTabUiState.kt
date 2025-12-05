package mega.privacy.android.feature.photos.presentation.timeline

import androidx.annotation.StringRes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.PhotoResult
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.model.Sort
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineSelectionMenuAction
import mega.privacy.android.shared.resources.R as sharedR

data class TimelineTabUiState(
    val isLoading: Boolean = true,
    val allPhotos: ImmutableList<PhotoResult> = persistentListOf(),
    val displayedPhotos: ImmutableList<PhotosNodeContentType> = persistentListOf(),
    val daysCardPhotos: ImmutableList<PhotosNodeListCard> = persistentListOf(),
    val monthsCardPhotos: ImmutableList<PhotosNodeListCard> = persistentListOf(),
    val yearsCardPhotos: ImmutableList<PhotosNodeListCard> = persistentListOf(),
    val gridSize: TimelineGridSize = TimelineGridSize.Default,
    val selectedPhotoCount: Int = 0,
    val currentSort: TimelineTabSortOptions = TimelineTabSortOptions.Newest,
    val isPaginationEnabled: Boolean = false,
    val selectedPhotosInTypedNode: ImmutableList<TypedNode> = persistentListOf(),
)

data class TimelineTabActionUiState(
    val normalModeItem: TimelineTabNormalModeActionUiState = TimelineTabNormalModeActionUiState(),
    val selectionModeItem: TimelineTabSelectionModeActionUiState = TimelineTabSelectionModeActionUiState(),
)

data class TimelineTabNormalModeActionUiState(
    val enableSort: Boolean = true,
)

data class TimelineTabSelectionModeActionUiState(
    val bottomBarActions: ImmutableList<TimelineSelectionMenuAction> = persistentListOf(),
    val bottomSheetActions: ImmutableList<TimelineSelectionMenuAction> = persistentListOf(),
)

enum class TimelineTabSortOptions(
    @StringRes val nameResId: Int,
    val sortOrder: SortOrder,
) {
    Newest(
        nameResId = sharedR.string.timeline_tab_sort_by_date_newest,
        sortOrder = SortOrder.ORDER_MODIFICATION_DESC
    ),
    Oldest(
        nameResId = sharedR.string.timeline_tab_sort_by_date_oldest,
        sortOrder = SortOrder.ORDER_MODIFICATION_ASC
    );

    companion object {
        fun TimelineTabSortOptions.toLegacySort() = when (this) {
            Newest -> Sort.NEWEST
            Oldest -> Sort.OLDEST
        }
    }
}

data class TimelineFilterUiState(
    val isRemembered: Boolean = false,
    val mediaType: FilterMediaType = FilterMediaType.ALL_MEDIA,
    val mediaSource: FilterMediaSource = FilterMediaSource.AllPhotos,
)
