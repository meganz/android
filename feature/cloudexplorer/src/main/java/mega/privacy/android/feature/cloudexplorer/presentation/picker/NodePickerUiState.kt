package mega.privacy.android.feature.cloudexplorer.presentation.picker

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.node.NodeId

/**
 * UI state shared by the explorer flows that only need a destination root to open at
 * (import, album import, select CU folder, share files to chat, add videos to playlist).
 */
@Stable
sealed interface NodePickerUiState {

    /**
     * Initial loading state.
     */
    data object Loading : NodePickerUiState

    /**
     * Data state.
     *
     * @property rootNodeId Root node id the explorer opens at.
     */
    data class Data(
        val rootNodeId: NodeId,
    ) : NodePickerUiState
}
