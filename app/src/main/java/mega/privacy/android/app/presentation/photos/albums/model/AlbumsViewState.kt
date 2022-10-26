package mega.privacy.android.app.presentation.photos.albums.model

import mega.privacy.android.domain.entity.photos.Album

/**
 * @property albums
 * @property currentAlbum
 * @property selectedPhotoIds
 */
data class AlbumsViewState(
    val albums: List<UIAlbum> = emptyList(),
    val currentAlbum: Album? = null,
    val selectedPhotoIds: MutableSet<Long> = mutableSetOf(),
)