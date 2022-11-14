package mega.privacy.android.app.presentation.photos.mediadiscovery.model

import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.model.UIPhoto
import mega.privacy.android.app.presentation.photos.model.ZoomLevel

data class MediaDiscoveryViewState(
    val uiPhotoList: List<UIPhoto> = emptyList(),
    val currentZoomLevel: ZoomLevel = ZoomLevel.Grid_3,
    val selectedPhotoIds: MutableSet<Long> = mutableSetOf(),
    val currentSort: Sort = Sort.NEWEST,
    val selectedTimeBarTab: TimeBarTab = TimeBarTab.All
)
