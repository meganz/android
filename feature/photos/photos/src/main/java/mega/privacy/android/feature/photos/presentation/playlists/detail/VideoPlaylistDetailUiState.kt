package mega.privacy.android.feature.photos.presentation.playlists.detail

import androidx.compose.runtime.Stable
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity

@Stable
sealed interface VideoPlaylistDetailUiState {
    data object Loading : VideoPlaylistDetailUiState

    data class Data(
        val currentPlaylist: VideoPlaylistUiEntity? = null,
    ) : VideoPlaylistDetailUiState
}


