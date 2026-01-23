package mega.privacy.android.feature.photos.presentation.playlists

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

/**
 * UI state for editing video playlist
 *
 * @property updateVideoPlaylistDialogEvent Event to show update video playlist dialog
 * @property updateTitleSuccessEvent Event to indicate that video playlist title update is successful
 * @property editVideoPlaylistErrorMessage Error message to show when editing video playlist fails
 * @property videoPlaylistPlaceholderTitle Placeholder title for video playlist
 */
data class VideoPlaylistEditState(
    val updateVideoPlaylistDialogEvent: StateEvent = consumed,
    val updateTitleSuccessEvent: StateEvent = consumed,
    val editVideoPlaylistErrorMessage: String? = null,
    val videoPlaylistPlaceholderTitle: String = "",
)
