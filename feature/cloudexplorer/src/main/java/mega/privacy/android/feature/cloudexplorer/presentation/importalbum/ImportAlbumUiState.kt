package mega.privacy.android.feature.cloudexplorer.presentation.importalbum

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.node.NodeId

/**
 * UI state for selecting a destination folder to import an album into.
 */
@Stable
sealed interface ImportAlbumUiState {

    /**
     * Initial loading state.
     */
    data object Loading : ImportAlbumUiState

    /**
     * Data state.
     *
     * @property rootNodeId Root node id.
     */
    data class Data(
        val rootNodeId: NodeId,
    ) : ImportAlbumUiState
}
