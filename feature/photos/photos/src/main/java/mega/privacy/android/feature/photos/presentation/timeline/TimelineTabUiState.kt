package mega.privacy.android.feature.photos.presentation.timeline

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.model.ZoomLevel
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard

data class TimelineTabUiState(
    val allPhotos: ImmutableList<PhotoUiState> = persistentListOf(),
    val displayedPhotos: ImmutableList<PhotosNodeContentType> = persistentListOf(),
    val daysCardPhotos: ImmutableList<PhotosNodeListCard> = persistentListOf(),
    val monthsCardPhotos: ImmutableList<PhotosNodeListCard> = persistentListOf(),
    val yearsCardPhotos: ImmutableList<PhotosNodeListCard> = persistentListOf(),
    val zoomLevel: ZoomLevel = ZoomLevel.Grid_3,
    val selectedPhotoCount: Int = 0,
    val currentSort: SortOrder = SortOrder.ORDER_MODIFICATION_DESC,
    val isPaginationEnabled: Boolean = false,
)
