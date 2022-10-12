package mega.privacy.android.app.presentation.photos.albums.model

import mega.privacy.android.domain.entity.photos.AlbumEntity

data class AlbumsViewState(
    val albums: List<UIAlbum> = emptyList(),
    val currentAlbum: AlbumEntity? = null
)