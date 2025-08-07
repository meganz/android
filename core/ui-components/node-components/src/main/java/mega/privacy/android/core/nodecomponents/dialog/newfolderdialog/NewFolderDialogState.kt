package mega.privacy.android.core.nodecomponents.dialog.newfolderdialog

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.node.NodeId

/**
 * State for the new folder dialog.
 */
data class NewFolderDialogState(
    val errorEvent: StateEventWithContent<Throwable> = consumed(),
    val folderCreatedEvent: StateEventWithContent<NodeId?> = consumed(),
)