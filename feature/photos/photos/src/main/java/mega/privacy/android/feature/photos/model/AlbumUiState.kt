package mega.privacy.android.feature.photos.model

import androidx.compose.runtime.Immutable

@Immutable
data class AlbumUiState(
    val id: Long,
    val title: String,
    val cover: PhotoUiState? = null
)