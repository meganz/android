package mega.privacy.android.feature.photos.presentation.albums

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState

data class AlbumsTabUiState(
    val albums: List<AlbumUiState> = emptyList(),
    val addNewAlbumErrorMessage: StateEventWithContent<String> = consumed(),
    val addNewAlbumSuccessEvent: StateEvent = consumed,
)