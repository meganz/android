package mega.privacy.android.app.presentation.photos.timeline.model

import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.domain.entity.photos.Photo

/**
 * View States for the Timeline View
 *
 * @property photos                                     A list of all photos
 * @property currentShowingPhotos                       A list of all shown photos(according to filter strategy)
 * @property photosListItems                            UI list items include custom separator
 * @property loadPhotosDone                             True if photos are loaded
 * @property yearsCardPhotos                            A list of the DateCard for year group
 * @property monthsCardPhotos                           A list of the DateCard for month group
 * @property daysCardPhotos                             A list of the DateCard for day group
 * @property timeBarTabs                                A list of all the time groups: Year, Month, Days, All
 * @property selectedTimeBarTab                         The selected time group
 * @property currentZoomLevel                           The current zoom level
 * @property scrollStartIndex                           Scroll start index
 * @property scrollStartOffset                          Scroll start index offset
 * @property applyFilterMediaType                       The current selected Media Type
 * @property currentFilterMediaType                     The current applied Media Type Filter
 * @property currentMediaSource                         The current applied Media Source Filter
 * @property currentSort                                The current applied sort
 * @property showingFilterPage                          True if the Filter view is open
 * @property showingSortByDialog                        True if the Sort dialog is being shown
 * @property enableZoomIn                               True if the Zoom In button is enabled
 * @property enableZoomOut                              True is the Zoom Out button is enabled
 * @property enableSortOption                           True is the Sort option menu is enabled
 * @property enableCameraUploadButtonShowing            True if we should show the Enable Camera Upload button
 * @property progressBarShowing                         True if we should show progress bar
 * @property progress                                   The current Camera Uploads progress
 * @property pending                                    The current number of pending items to be uploaded
 * @property enableCameraUploadPageShowing              True if the enable CU view is on
 * @property cuUploadsVideos                            True if videos is selected as being uploaded
 * @property cuUseCellularConnection                    True if use cellular connection is selected
 * @property selectedPhotoCount                         Selected photo count
 * @property selectedPhoto                              Selected photo
 * @property shouldTriggerCameraUploads                 True if Camera Uploads can be triggered
 * @property shouldShowBusinessAccountPrompt            True if the Business Account prompt should be shown
 * @property shouldShowBusinessAccountSuspendedPrompt   True if the Suspended Business Account prompt should be shown
 * @property shouldTriggerMediaPermissionsDeniedLogic   True if certain logic should be executed when Media Permissions are denied
 */
data class TimelineViewState(
    val photos: List<Photo> = emptyList(),
    val currentShowingPhotos: List<Photo> = emptyList(),
    val photosListItems: List<PhotoListItem> = emptyList(),
    val loadPhotosDone: Boolean = false,
    val yearsCardPhotos: List<DateCard> = emptyList(),
    val monthsCardPhotos: List<DateCard> = emptyList(),
    val daysCardPhotos: List<DateCard> = emptyList(),
    val timeBarTabs: List<TimeBarTab> = TimeBarTab.values().asList(),
    val selectedTimeBarTab: TimeBarTab = TimeBarTab.All,
    val currentZoomLevel: ZoomLevel = ZoomLevel.Grid_3,
    val scrollStartIndex: Int = 0,
    val scrollStartOffset: Int = 0,
    val applyFilterMediaType: ApplyFilterMediaType = ApplyFilterMediaType.ALL_MEDIA_IN_CD_AND_CU,
    val currentFilterMediaType: FilterMediaType = FilterMediaType.ALL_MEDIA,
    val currentMediaSource: TimelinePhotosSource = TimelinePhotosSource.ALL_PHOTOS,
    val currentSort: Sort = Sort.NEWEST,
    val showingFilterPage: Boolean = false,
    val showingSortByDialog: Boolean = false,
    val enableZoomIn: Boolean = true,
    val enableZoomOut: Boolean = true,
    val enableSortOption: Boolean = true,
    val enableCameraUploadButtonShowing: Boolean = true,
    val progressBarShowing: Boolean = false,
    val progress: Float = 0f,
    val pending: Int = 0,
    val enableCameraUploadPageShowing: Boolean = false,
    val cuUploadsVideos: Boolean = false,
    val cuUseCellularConnection: Boolean = false,
    val selectedPhotoCount: Int = 0,
    val selectedPhoto: Photo? = null,
    val shouldTriggerCameraUploads: Boolean = false,
    val shouldShowBusinessAccountPrompt: Boolean = false,
    val shouldShowBusinessAccountSuspendedPrompt: Boolean = false,
    val shouldTriggerMediaPermissionsDeniedLogic: Boolean = false,
)