package mega.privacy.android.feature.photos.presentation.playlists

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.videosection.UserVideoPlaylist

/**
 * UI state for editing video playlist
 *
 * @property showUpdateVideoPlaylist Event to show update video playlist dialog
 * @property showCreateVideoPlaylist Event to show create video playlist dialog
 * @property createVideoPlaylistSuccessEvent Event to indicate that video playlist creation is successful
 * @property updateTitleSuccessEvent Event to indicate that video playlist title update is successful
 * @property editVideoPlaylistErrorMessage Error message to show when editing video playlist fails
 * @property playlistsRemovedEvent Event for playlists removed
 * @property numberOfRemovedVideosEvent Event for number of videos removed from playlist
 */
data class VideoPlaylistEditState(
    val showUpdateVideoPlaylist: Boolean = false,
    val showCreateVideoPlaylist: Boolean = false,
    val createVideoPlaylistSuccessEvent: StateEventWithContent<UserVideoPlaylist> = consumed(),
    val updateTitleSuccessEvent: StateEvent = consumed,
    val editVideoPlaylistErrorMessage: String? = null,
    val playlistsRemovedEvent: StateEventWithContent<List<String>> = consumed(),
    val numberOfRemovedVideosEvent: StateEventWithContent<Int> = consumed(),
)
