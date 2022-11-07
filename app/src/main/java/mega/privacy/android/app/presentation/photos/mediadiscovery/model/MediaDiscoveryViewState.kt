package mega.privacy.android.app.presentation.photos.mediadiscovery.model

import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.model.UIPhoto
import mega.privacy.android.app.presentation.photos.model.ZoomLevel

data class MediaDiscoveryViewState(
    val uiPhotoList: List<UIPhoto> = emptyList(),
    val currentZoomLevel: ZoomLevel = ZoomLevel.Grid_3,
    val selectedPhotoIds: Set<Long> = emptySet(),
    val currentSort: Sort = Sort.NEWEST,
    val selectedTimeBarTab: TimeBarTab = TimeBarTab.All,
    val yearsCardList: List<DateCard> = emptyList(),
    val monthsCardList: List<DateCard> = emptyList(),
    val daysCardList: List<DateCard> = emptyList(),
    val scrollStartIndex: Int = 0,
    val scrollStartOffset: Int = 0,
)
