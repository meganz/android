package mega.privacy.android.feature.photos.presentation.albums

import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.navigation.destination.AlbumGetMultipleLinksNavKey

data class AlbumsTabUiState(
    val albums: List<AlbumUiState> = emptyList(),
    val selectedUserAlbums: Set<MediaAlbum.User> = emptySet(),
    val addNewAlbumErrorMessage: StateEventWithContent<String> = consumed(),
    val addNewAlbumSuccessEvent: StateEventWithContent<AlbumId> = consumed(),
    val navigationEvent: StateEventWithContent<NavKey> = consumed(),
    val deleteAlbumsConfirmationEvent: StateEvent = consumed,
) {
    val selectedUserAlbumsCount
        get() = selectedUserAlbums.size

    val isInSelectionMode
        get() = selectedUserAlbums.isNotEmpty()
}