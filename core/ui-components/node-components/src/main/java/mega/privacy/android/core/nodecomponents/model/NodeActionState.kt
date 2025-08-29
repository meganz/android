package mega.privacy.android.core.nodecomponents.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * State for NodeActionsViewModel
 *
 * @property selectedNodes Selected nodes
 * @property error Error
 * @property nodeNameCollisionsResult Node name collision result
 * @property showForeignNodeDialog Show foreign node dialog
 * @property showQuotaDialog Show quota dialog
 * @property accessPermissionIcon Access permission icon
 * @property shareInfo Share info
 * @property outgoingShares Outgoing shares
 * @property contactsData Contacts data
 * @property downloadEvent Download event
 * @property selectAll Select All event
 * @property clearAll clear All Event
 * @property infoToShowEvent Info to show event
 */
data class NodeActionState(
    val selectedNodes: List<TypedNode> = emptyList(),
    val error: StateEventWithContent<Throwable> = consumed(),
    val nodeNameCollisionsResult: StateEventWithContent<NodeNameCollisionsResult> = consumed(),
    val showForeignNodeDialog: StateEvent = consumed,
    val showQuotaDialog: StateEventWithContent<Boolean> = consumed(),
    val accessPermissionIcon: Int? = null,
    val shareInfo: String? = null,
    val outgoingShares: List<ShareData> = emptyList(),
    val contactsData: StateEventWithContent<Triple<List<String>, Boolean, String>> = consumed(),
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val selectAll: StateEvent = consumed,
    val clearAll: StateEvent = consumed,
    val infoToShowEvent: StateEventWithContent<LocalizedText> = consumed(),
    val renameNodeRequestEvent: StateEventWithContent<NodeId> = consumed(),
    val shareFolderDialogEvent: StateEventWithContent<List<Long>> = consumed(),
    val shareFolderEvent: StateEventWithContent<List<Long>> = consumed()
)