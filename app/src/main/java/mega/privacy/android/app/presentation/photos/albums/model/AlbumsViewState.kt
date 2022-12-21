package mega.privacy.android.app.presentation.photos.albums.model

import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.domain.entity.photos.Album

/**
 * @property albums
 * @property currentAlbum
 * @property selectedPhotoIds
 * @property currentSort
 */
data class AlbumsViewState(
    val albums: List<UIAlbum> = emptyList(),
    val currentAlbum: Album? = null,
    val selectedPhotoIds: Set<Long> = emptySet(),
    val currentSort: Sort = Sort.NEWEST,
)