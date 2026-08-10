package mega.privacy.android.feature.cloudexplorer.presentation.picker

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType

/**
 * UI state shared by the copy and move flows, which resume at the last picked destination.
 */
@Stable
sealed interface TargetNodePickerUiState {

    /**
     * Initial loading state.
     */
    data object Loading : TargetNodePickerUiState

    /**
     * Data state.
     *
     * @property rootNodeId Root node id the explorer opens at.
     * @property targetPath Folders to push onto the back stack (top-down) to resume at the last
     *   destination. Empty when there is no valid last target.
     * @property nodeSourceType Node source type the [targetPath] lives under (Cloud Drive or
     *   Incoming Shares), used to seed the back stack with the correct explorer source.
     */
    data class Data(
        val rootNodeId: NodeId,
        val targetPath: List<NodeId> = emptyList(),
        val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    ) : TargetNodePickerUiState
}
