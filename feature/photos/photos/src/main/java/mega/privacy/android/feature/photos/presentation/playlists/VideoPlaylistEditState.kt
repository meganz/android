package mega.privacy.android.feature.photos.presentation.playlists

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * UI state for editing video playlist
 *
 * @property showUpdateVideoPlaylistDialog Event to show update video playlist dialog
 * @property updateTitleSuccessEvent Event to indicate that video playlist title update is successful
 * @property editVideoPlaylistErrorMessage Error message to show when editing video playlist fails
 * @property videoPlaylistPlaceholderTitle Placeholder title for video playlist
 * @property playlistsRemovedEvent Event for playlists removed
 */
data class VideoPlaylistEditState(
    val showUpdateVideoPlaylistDialog: Boolean = false,
    val updateTitleSuccessEvent: StateEvent = consumed,
    val editVideoPlaylistErrorMessage: String? = null,
    val videoPlaylistPlaceholderTitle: String = "",
    val playlistsRemovedEvent: StateEventWithContent<List<String>> = consumed(),
)
