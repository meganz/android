package mega.privacy.android.feature.cloudexplorer.presentation.addvideotoplaylist

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.node.NodeId

/**
 * UI state for selecting videos to add to a playlist.
 */
@Stable
sealed interface AddVideoToPlaylistUiState {

    /**
     * Initial loading state.
     */
    data object Loading : AddVideoToPlaylistUiState

    /**
     * Data state.
     *
     * @property rootNodeId Root node id.
     */
    data class Data(
        val rootNodeId: NodeId,
    ) : AddVideoToPlaylistUiState
}
