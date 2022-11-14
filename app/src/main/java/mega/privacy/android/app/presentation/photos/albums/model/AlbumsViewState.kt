package mega.privacy.android.app.presentation.photos.albums.model

import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.domain.entity.photos.Album

/**
 * @property albums
 * @property currentAlbumId
 * @property selectedPhotoIds
 * @property currentSort
 */
data class AlbumsViewState(
    val albums: List<UIAlbum> = emptyList(),
    val currentAlbumId: Album? = null,
    val selectedPhotoIds: MutableSet<Long> = mutableSetOf(),
    val currentSort: Sort = Sort.NEWEST,
)