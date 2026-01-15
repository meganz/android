package mega.privacy.android.feature.photos.presentation.albums.coverselection

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentType

data class AlbumCoverSelectionState(
    val album: Album.UserAlbum? = null,
    val isInvalidAlbum: Boolean = false,
    val photos: List<PhotoUiState> = emptyList(),
    val photosNodeContentTypes: List<PhotosNodeContentType> = emptyList(),
    val hasSelectedPhoto: Boolean = false,
    val isSelectionCompleted: Boolean = false,
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
    val hiddenNodeEnabled: Boolean = false,
)

typealias PhotoDownload = suspend (
    photo: Photo,
    callback: (success: Boolean) -> Unit,
) -> Unit
