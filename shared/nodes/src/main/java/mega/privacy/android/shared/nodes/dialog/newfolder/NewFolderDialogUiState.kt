package mega.privacy.android.shared.nodes.dialog.newfolder

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.node.NodeId

/**
 * State for the new folder dialog.
 */
data class NewFolderDialogUiState(
    val errorEvent: StateEventWithContent<Throwable> = consumed(),
    val folderCreatedEvent: StateEventWithContent<NodeId?> = consumed(),
    val dismissOnDisconnectionEvent: StateEvent = consumed,
)