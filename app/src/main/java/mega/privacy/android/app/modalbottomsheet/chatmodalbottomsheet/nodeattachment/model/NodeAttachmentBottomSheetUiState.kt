package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * UI state for the OfflineFileInfoComposeViewModel
 *
 * @property nodeId NodeId
 * @property item ChatAttachmentUiEntity
 * @property isOnline true if connected to network
 * @property isLoading true if the node is still loading, false otherwise
 * @property errorEvent event to show an error message
 */
data class NodeAttachmentBottomSheetUiState(
    val item: ChatAttachmentUiEntity? = null,
    val isOnline: Boolean = false,
    val isLoading: Boolean = true,
    val errorEvent: StateEventWithContent<Boolean> = consumed(),
)