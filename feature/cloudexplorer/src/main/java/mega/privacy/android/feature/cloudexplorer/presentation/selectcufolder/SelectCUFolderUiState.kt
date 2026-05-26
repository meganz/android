package mega.privacy.android.feature.cloudexplorer.presentation.selectcufolder

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.node.NodeId

/**
 * UI state for selecting CU folder.
 */
@Stable
sealed interface SelectCUFolderUiState {

    /**
     * Initial loading state.
     */
    data object Loading : SelectCUFolderUiState

    /**
     * Data state.
     *
     * @property rootNodeId Root node id.
     */
    data class Data(
        val rootNodeId: NodeId,
    ) : SelectCUFolderUiState
}
