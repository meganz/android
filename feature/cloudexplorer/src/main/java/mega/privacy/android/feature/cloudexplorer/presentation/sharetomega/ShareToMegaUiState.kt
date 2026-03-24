package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEventWithContent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

@Stable
sealed interface ShareToMegaUiState {

    /**
     * Initial loading state.
     */
    data object Loading : ShareToMegaUiState

    /**
     * Data state.
     *
     * @property rootNodeId Root node id.
     */
    data class Data(
        val rootNodeId: NodeId,
        val openFolderEvent: StateEventWithContent<NodeId>,
        val uploadEvent: StateEventWithContent<TransferTriggerEvent>,
    ) : ShareToMegaUiState
}
