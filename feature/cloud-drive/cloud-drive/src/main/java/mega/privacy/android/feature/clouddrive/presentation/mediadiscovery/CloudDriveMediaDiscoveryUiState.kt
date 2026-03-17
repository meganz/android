package mega.privacy.android.feature.clouddrive.presentation.mediadiscovery

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.photos.DateCard
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.domain.entity.photos.MediaListItem
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.Sort
import mega.privacy.android.domain.entity.photos.ZoomLevel
import mega.privacy.android.feature.clouddrive.presentation.mediadiscovery.model.MediaDiscoveryPeriod

data class CloudDriveMediaDiscoveryUiState(
    val backEvent: StateEvent = consumed,
    val loadPhotosDone: Boolean = false,
    val sourcePhotos: List<Photo> = emptyList(),
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
    val selectedPeriod: MediaDiscoveryPeriod = MediaDiscoveryPeriod.All,
    val scrollStartIndex: Int = 0,
    val scrollStartOffset: Int = 0
)