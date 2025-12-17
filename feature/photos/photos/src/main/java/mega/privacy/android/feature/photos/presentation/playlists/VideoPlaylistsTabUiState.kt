package mega.privacy.android.feature.photos.presentation.playlists

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity

@Stable
sealed interface VideoPlaylistsTabUiState {
    data object Loading : VideoPlaylistsTabUiState

    /**
     * The state is for the video playlists section
     *
     * @property videoPlaylistEntities the video playlist entities
     * @property videoPlaylists the video playlists
     */
    data class Data(
        val videoPlaylistEntities: List<VideoPlaylistUiEntity> = emptyList(),
        val videoPlaylists: List<VideoPlaylist> = emptyList()
    ) : VideoPlaylistsTabUiState
}
