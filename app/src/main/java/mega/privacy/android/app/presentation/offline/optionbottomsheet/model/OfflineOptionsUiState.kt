package mega.privacy.android.app.presentation.offline.optionbottomsheet.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OfflineFileInformation

/**
 * UI state for the OfflineFileInfoComposeViewModel
 *
 * @property nodeId NodeId
 * @property offlineFileInformation OfflineFileInformation
 * @property isOnline true if connected to network
 * @property isLoading true if the node is still loading, false otherwise
 * @property errorEvent event to show an error message
 */
data class OfflineOptionsUiState(
    val nodeId: NodeId,
    val offlineFileInformation: OfflineFileInformation? = null,
    val isOnline: Boolean = false,
    val isLoading: Boolean = true,
    val errorEvent: StateEventWithContent<Boolean> = consumed(),
)