package mega.privacy.android.app.presentation.photos.albums.model

import mega.privacy.android.domain.entity.photos.Album

data class AlbumsViewState(
    val albums: List<Album> = emptyList(),
)