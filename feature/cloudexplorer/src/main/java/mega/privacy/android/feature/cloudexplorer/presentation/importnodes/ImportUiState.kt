package mega.privacy.android.feature.cloudexplorer.presentation.importnodes

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.node.NodeId

/**
 * UI state for selecting a destination folder to import nodes into.
 */
@Stable
sealed interface ImportUiState {

    /**
     * Initial loading state.
     */
    data object Loading : ImportUiState

    /**
     * Data state.
     *
     * @property rootNodeId Root node id.
     */
    data class Data(
        val rootNodeId: NodeId,
    ) : ImportUiState
}
