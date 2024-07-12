package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState

/**
 * The state is for the videos section
 *
 * @property allVideos the all video items
 * @property sortOrder the sort order of video items
 * @property isPendingRefresh
 * @property progressBarShowing the progress bar showing state
 * @property scrollToTop the scroll to top state
 * @property selectedVideoHandles the selected video handles
 * @property selectedVideoPlaylistHandles the selected video playlist handles
 * @property selectedVideoElementIDs the selected video element ids
 * @property locationSelectedFilterOption the location selected filter option
 * @property durationSelectedFilterOption the duration selected filter option
 * @property isInSelection if list is in selection mode or not
 * @property videoPlaylists the video playlists
 * @property currentVideoPlaylist the current video playlist
 * @property isVideoPlaylistCreatedSuccessfully the video playlist created successfully state
 * @property numberOfAddedVideos the number of added videos
 * @property numberOfRemovedItems the number of removed items
 * @property isPlaylistProgressBarShown true if the playlist progress bar is being shown
 * @property isInputTitleValid true if the input title is valid
 * @property createVideoPlaylistPlaceholderTitle the create video playlist placeholder title
 * @property createDialogErrorMessage the create dialog error message
 * @property deletedVideoPlaylistTitles the deleted video playlist titles
 * @property areVideoPlaylistsRemovedSuccessfully true if the video playlists are removed successfully
 * @property currentDestinationRoute the current destination route
 * @property updateToolbarTitle true is to update toolbar title
 * @property accountDetail the account detail
 * @property isHiddenNodesOnboarded if is hidden nodes onboarded
 * @property clickedItem the clicked item
 * @property clickedPlaylistDetailItem the clicked playlist detail item
 * @property searchState the search state
 * @property query the search query
 * @property isHideMenuActionVisible the hide menu action whether is visible
 * @property isUnhideMenuActionVisible the unhide menu action whether is visible
 * @property isRemoveLinkMenuActionVisible the remove link menu action whether is visible
 */
data class VideoSectionState(
    val allVideos: List<VideoUIEntity> = emptyList(),
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val isPendingRefresh: Boolean = false,
    val progressBarShowing: Boolean = true,
    val scrollToTop: Boolean = false,
    val selectedVideoHandles: List<Long> = emptyList(),
    val selectedVideoPlaylistHandles: List<Long> = emptyList(),
    val selectedVideoElementIDs: List<Long> = emptyList(),
    val locationSelectedFilterOption: LocationFilterOption = LocationFilterOption.AllLocations,
    val durationSelectedFilterOption: DurationFilterOption = DurationFilterOption.AllDurations,
    val isInSelection: Boolean = false,
    val videoPlaylists: List<VideoPlaylistUIEntity> = emptyList(),
    val currentVideoPlaylist: VideoPlaylistUIEntity? = null,
    val isVideoPlaylistCreatedSuccessfully: Boolean = false,
    val numberOfAddedVideos: Int = 0,
    val numberOfRemovedItems: Int = 0,
    val isPlaylistProgressBarShown: Boolean = true,
    val isInputTitleValid: Boolean = true,
    val createVideoPlaylistPlaceholderTitle: String = "",
    val createDialogErrorMessage: Int? = null,
    val deletedVideoPlaylistTitles: List<String> = emptyList(),
    val areVideoPlaylistsRemovedSuccessfully: Boolean = false,
    val currentDestinationRoute: String? = null,
    val updateToolbarTitle: String? = null,
    val accountDetail: AccountDetail? = null,
    val isHiddenNodesOnboarded: Boolean = false,
    val clickedItem: TypedVideoNode? = null,
    val clickedPlaylistDetailItem: TypedVideoNode? = null,
    val searchState: SearchWidgetState = SearchWidgetState.COLLAPSED,
    val query: String? = null,
    val isHideMenuActionVisible: Boolean = false,
    val isUnhideMenuActionVisible: Boolean = false,
    val isRemoveLinkMenuActionVisible: Boolean = false,
)
