package mega.privacy.android.feature.photos.presentation.mediadiscovery

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.DateCard
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.domain.entity.photos.MediaListItem
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.Sort
import mega.privacy.android.domain.entity.photos.ZoomLevel
import mega.privacy.android.feature.photos.presentation.timeline.model.MediaTimePeriod

data class CloudDriveMediaDiscoveryUiState(
    val backEvent: StateEvent = consumed,
    val loadPhotosDone: Boolean = false,
    val selectedPhotoIds: Set<Long> = emptySet(),
    val sourcePhotos: List<Photo> = emptyList(),
    val sourceNodes: List<TypedFileNode> = emptyList(),
    val mediaListItemList: List<MediaListItem> = emptyList(),
    val currentZoomLevel: ZoomLevel = ZoomLevel.Grid_3,
    val currentSort: Sort = Sort.NEWEST,
    val currentMediaType: FilterMediaType = FilterMediaType.ALL_MEDIA,
    val yearsCardList: List<DateCard> = emptyList(),
    val monthsCardList: List<DateCard> = emptyList(),
    val daysCardList: List<DateCard> = emptyList(),
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
    val isHiddenNodesEnabled: Boolean = false,
    val showHiddenNodes: Boolean = false,
    val selectedPeriod: MediaTimePeriod = MediaTimePeriod.All,
    val scrollStartIndex: Int = 0,
    val scrollStartOffset: Int = 0,
    val fromFolderLink: Boolean = false,
    val folderName: String = "",
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    val hasWritePermission: Boolean = false,
) {
    val selectedNodes: Set<TypedFileNode>
        get() = sourceNodes
            .filter { it.id.longValue in selectedPhotoIds }
            .toSet()

    val isInSelectionMode = selectedPhotoIds.isNotEmpty()
    val selectedPhotosCount = selectedPhotoIds.size
    val isAllSelected = selectedPhotoIds.size == sourcePhotos.size
    val isUploadAllowed = hasWritePermission
            && nodeSourceType != NodeSourceType.RUBBISH_BIN
            && !isInSelectionMode
}
