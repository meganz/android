package mega.privacy.android.app.presentation.photos.mediadiscovery.model

import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.model.UIPhoto
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.domain.entity.photos.Photo

/**
 * Media Discovery View state
 *
 * @property sourcePhotos the photos from sdk
 * @property uiPhotoList photo list
 * @property currentZoomLevel current zoom level
 * @property selectedPhotoIds selected photo ids
 * @property currentSort current sort
 * @property currentMediaType
 * @property selectedTimeBarTab selected time bar tab
 * @property yearsCardList years card list
 * @property monthsCardList months card list
 * @property daysCardList days card list
 * @property scrollStartIndex the start index of scroll
 * @property scrollStartOffset the start offset of scroll
 * @property mediaDiscoveryViewSettings media discovery dialog view settings
 * @property shouldBack handle empty state, when no photos then back to file list page, eg, delete.
 * @property showSortByDialog
 * @property showFilterDialog
 * @property showSlidersPopup
 * @property collisions
 * @property copyThrowable
 * @property copyResultText
 * @property isConnectedToNetwork
 * @property hasDbCredentials
 * @property loadPhotosDone
 */
data class MediaDiscoveryViewState(
    val sourcePhotos: List<Photo> = emptyList(),
    val uiPhotoList: List<UIPhoto> = emptyList(),
    val currentZoomLevel: ZoomLevel = ZoomLevel.Grid_3,
    val selectedPhotoIds: Set<Long> = emptySet(),
    val currentSort: Sort = Sort.NEWEST,
    val currentMediaType: FilterMediaType = FilterMediaType.ALL_MEDIA,
    val selectedTimeBarTab: TimeBarTab = TimeBarTab.All,
    val yearsCardList: List<DateCard> = emptyList(),
    val monthsCardList: List<DateCard> = emptyList(),
    val daysCardList: List<DateCard> = emptyList(),
    val scrollStartIndex: Int = 0,
    val scrollStartOffset: Int = 0,
    val mediaDiscoveryViewSettings: Int? = null,
    val shouldBack: Boolean = false,
    val showSortByDialog: Boolean = false,
    val showFilterDialog: Boolean = false,
    val showSlidersPopup: Boolean = false,
    val collisions: ArrayList<NameCollision>? = null,
    val copyThrowable: Throwable? = null,
    val copyResultText: String? = null,
    val isConnectedToNetwork: Boolean = true,
    val hasDbCredentials: Boolean = true,
    val loadPhotosDone: Boolean = false,
)
