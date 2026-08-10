package mega.privacy.android.feature.photos.presentation.playlists

import androidx.compose.runtime.Stable
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity

@Stable
sealed interface VideoPlaylistsTabUiState {
    data object Loading : VideoPlaylistsTabUiState

    /**
     * The state is for the video playlists section
     *
     * @property videoPlaylistEntities the video playlist entities
     * @property selectedSortConfiguration the selected sort configuration
     * @property selectedPlaylists the selected playlists
     * @property query the search query
     */
    data class Data(
        val videoPlaylistEntities: List<VideoPlaylistUiEntity> = emptyList(),
        val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
        val selectedPlaylists: Set<VideoPlaylistUiEntity> = emptySet(),
        val query: String? = null,
    ) : VideoPlaylistsTabUiState {

        /**
         * The selection mode whether is enabled
         */
        val isSelectionMode get() = selectedPlaylists.isNotEmpty()
    }
}
