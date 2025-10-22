package mega.privacy.android.feature.photos.presentation.albums

import mega.privacy.android.domain.entity.photos.Album

data class AlbumsTabUiState(
    val albums: List<Album.UserAlbum> = emptyList()
)