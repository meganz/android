package mega.privacy.android.app.presentation.photos.albums.photosselection

import mega.privacy.android.app.presentation.photos.model.UIPhoto
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo

data class AlbumPhotosSelectionState(
    val album: Album.UserAlbum? = null,
    val isInvalidAlbum: Boolean = false,
    val albumPhotoIds: Set<Long> = setOf(),
    val photos: List<Photo> = listOf(),
    val uiPhotos: List<UIPhoto> = listOf(),
    val selectedPhotoIds: Set<Long> = setOf(),
    val selectedLocation: TimelinePhotosSource = TimelinePhotosSource.ALL_PHOTOS,
    val isLocationDetermined: Boolean = false,
    val showFilterMenu: Boolean = false,
    val isSelectionCompleted: Boolean = false,
)

typealias PhotoDownload = suspend (
    photo: Photo,
    callback: (success: Boolean) -> Unit,
) -> Unit
