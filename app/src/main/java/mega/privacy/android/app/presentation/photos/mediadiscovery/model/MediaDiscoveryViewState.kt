package mega.privacy.android.app.presentation.photos.mediadiscovery.model

import androidx.annotation.StringRes
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.core.nodecomponents.components.banners.StorageOverQuotaCapacity
import mega.privacy.android.core.nodecomponents.components.banners.StorageOverQuotaCapacity.DEFAULT
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.MediaListItem
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * Media Discovery View state
 *
 * @property currentFolderId current folder id
 * @property sourcePhotos the photos from sdk
 * @property mediaListItemList photo list
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
 * @property showSortByDialog
 * @property showFilterDialog
 * @property showSlidersPopup
 * @property collisions
 * @property copyThrowable
 * @property copyResultText
 * @property isConnectedToNetwork
 * @property hasDbCredentials
 * @property loadPhotosDone
 * @property shouldGoBack when current folder is deleted, should automatic go back
 * @property downloadEvent event to trigger the download
 * @property errorMessage The [StringRes] of the message to display in the error banner
 * @property accountType the account detail
 * @property isHiddenNodesOnboarded if is hidden nodes onboarded
 * @property storageCapacity the storage capacity
 * @property isBusinessAccountExpired if the business or pro flexi is expired
 * @property hiddenNodeEnabled if hidden node is enabled
 */
data class MediaDiscoveryViewState(
    val currentFolderId: Long? = null,
    val sourcePhotos: List<Photo> = emptyList(),
    val mediaListItemList: List<MediaListItem> = emptyList(),
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
    val showSortByDialog: Boolean = false,
    val showFilterDialog: Boolean = false,
    val showSlidersPopup: Boolean = false,
    val collisions: List<NameCollision> = emptyList(),
    val copyThrowable: Throwable? = null,
    val copyResultText: String? = null,
    val isConnectedToNetwork: Boolean = true,
    val hasDbCredentials: Boolean = true,
    val loadPhotosDone: Boolean = false,
    val shouldGoBack: Boolean = false,
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    @StringRes val errorMessage: Int? = null,
    val accountType: AccountType? = null,
    val isHiddenNodesOnboarded: Boolean = false,
    val storageCapacity: StorageOverQuotaCapacity = DEFAULT,
    val isBusinessAccountExpired: Boolean = false,
    val hiddenNodeEnabled: Boolean = false,
    val isClearSelectedPhotos: Boolean = true,
)
