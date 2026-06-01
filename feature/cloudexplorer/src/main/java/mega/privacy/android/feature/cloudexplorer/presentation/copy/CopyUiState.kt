package mega.privacy.android.feature.cloudexplorer.presentation.copy

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType

/**
 * UI state for selecting a destination folder to copy nodes into.
 */
@Stable
sealed interface CopyUiState {

    /**
     * Initial loading state.
     */
    data object Loading : CopyUiState

    /**
     * Data state.
     *
     * @property rootNodeId Root node id the explorer opens at.
     * @property targetPath Folders to push onto the back stack (top-down) to resume at the last copy
     *   destination. Empty when there is no valid last target.
     * @property nodeSourceType Node source type the [targetPath] lives under (Cloud Drive or
     *   Incoming Shares), used to seed the back stack with the correct explorer source.
     */
    data class Data(
        val rootNodeId: NodeId,
        val targetPath: List<NodeId> = emptyList(),
        val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    ) : CopyUiState
}
