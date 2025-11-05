package mega.privacy.android.feature.photos.presentation.albums.model

import androidx.compose.runtime.Immutable
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.feature.photos.model.PhotoUiState

@Immutable
data class AlbumUiState(
    val mediaAlbum: MediaAlbum,
    val title: String,
    val cover: PhotoUiState? = null
)