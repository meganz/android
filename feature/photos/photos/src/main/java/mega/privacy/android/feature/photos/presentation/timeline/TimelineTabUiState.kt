package mega.privacy.android.feature.photos.presentation.timeline

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.photos.PhotoResult
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.feature.photos.model.PhotosNodeContentItem
import mega.privacy.android.domain.entity.photos.Sort
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import mega.privacy.android.shared.resources.R as sharedR

data class TimelineTabUiState(
    val isLoading: Boolean = true,
    val allPhotos: List<PhotoResult> = emptyList(),
    val displayedPhotos: List<PhotosNodeContentItem> = emptyList(),
    val daysCardPhotos: List<PhotosNodeListCard> = emptyList(),
    val monthsCardPhotos: List<PhotosNodeListCard> = emptyList(),
    val yearsCardPhotos: List<PhotosNodeListCard> = emptyList(),
    val gridSize: TimelineGridSize = TimelineGridSize.Default,
    val currentSort: TimelineTabSortOptions = TimelineTabSortOptions.Newest,
)

data class TimelineTabActionUiState(
    val isReady: Boolean = false,
    val normalModeItem: TimelineTabNormalModeActionUiState = TimelineTabNormalModeActionUiState(),
)

data class TimelineTabNormalModeActionUiState(
    val enableSort: Boolean = true,
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
