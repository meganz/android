package mega.privacy.android.feature.photos.presentation.playlists.detail

import androidx.compose.runtime.Stable
import mega.privacy.android.feature.photos.presentation.playlists.detail.model.VideoPlaylistDetailUiEntity

@Stable
sealed interface VideoPlaylistDetailUiState {
    data object Loading : VideoPlaylistDetailUiState

    /**
     * The state is for the video playlist detail screen
     *
     * @property playlistDetail The video playlist detail ui entity
     */
    data class Data(
        val playlistDetail: VideoPlaylistDetailUiEntity? = null,
    ) : VideoPlaylistDetailUiState
}


