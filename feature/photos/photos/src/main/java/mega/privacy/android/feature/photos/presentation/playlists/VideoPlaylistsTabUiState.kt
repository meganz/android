package mega.privacy.android.feature.photos.presentation.playlists

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.SortOrder
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
     * @property sortOrder the sort order
     * @property selectedSortConfiguration the selected sort configuration
     * @property selectedPlaylists the selected playlists
     * @property playlistsRemovedEvent event for playlists removed
     * @property query the search query
     */
    data class Data(
        val videoPlaylistEntities: List<VideoPlaylistUiEntity> = emptyList(),
        val videoPlaylists: List<VideoPlaylist> = emptyList(),
        val sortOrder: SortOrder = SortOrder.ORDER_NONE,
        val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
        val selectedPlaylists: Set<VideoPlaylistUiEntity> = emptySet(),
        val playlistsRemovedEvent: StateEventWithContent<List<String>> = consumed(),
        val query: String? = null,
    ) : VideoPlaylistsTabUiState
}
