package mega.privacy.android.feature.photos.presentation.albums

import mega.privacy.android.feature.photos.model.AlbumUiState

data class AlbumsTabUiState(
    val albums: List<AlbumUiState> = emptyList()
)