package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.videosection.VideoPlaylist

/**
 * The state is for the videos section
 *
 * @property allVideos the all video items
 * @property sortOrder the sort order of video items
 * @property isPendingRefresh
 * @property progressBarShowing the progress bar showing state
 * @property searchMode the search mode state
 * @property scrollToTop the scroll to top state
 * @property selectedVideoHandles the selected video handles
 * @property isInSelection if list is in selection mode or not
 * @property videoPlaylists the video playlists
 * @property currentVideoPlaylist the current video playlist
 * @property isVideoPlaylistCreatedSuccessfully the video playlist created successfully state
 * @property numberOfAddedVideos the number of added videos
 * @property isPlaylistProgressBarShown true if the playlist progress bar is being shown
 */
data class VideoSectionState(
    val allVideos: List<UIVideo> = emptyList(),
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val isPendingRefresh: Boolean = false,
    val progressBarShowing: Boolean = true,
    val searchMode: Boolean = false,
    val scrollToTop: Boolean = false,
    val selectedVideoHandles: List<Long> = emptyList(),
    val isInSelection: Boolean = false,
    val videoPlaylists: List<UIVideoPlaylist> = emptyList(),
    val currentVideoPlaylist: VideoPlaylist? = null,
    val isVideoPlaylistCreatedSuccessfully: Boolean = false,
    val numberOfAddedVideos: Int = 0,
    val isPlaylistProgressBarShown: Boolean = true,
)
