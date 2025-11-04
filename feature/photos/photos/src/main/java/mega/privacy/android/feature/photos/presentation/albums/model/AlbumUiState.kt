package mega.privacy.android.feature.photos.presentation.albums.model

import androidx.compose.runtime.Immutable
import mega.privacy.android.feature.photos.model.PhotoUiState

@Immutable
data class AlbumUiState(
    val id: Long,
    val title: String,
    val cover: PhotoUiState? = null
)