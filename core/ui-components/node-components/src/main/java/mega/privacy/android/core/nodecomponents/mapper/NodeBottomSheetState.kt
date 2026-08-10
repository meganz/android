package mega.privacy.android.core.nodecomponents.mapper

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import kotlinx.collections.immutable.persistentListOf
import mega.privacy.android.core.nodecomponents.model.NodeActionModeMenuItem
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.shared.nodes.model.NodeUiItem

/**
 * Node bottom sheet state
 *
 * @property nodeId The unique identifier of the node
 * @property nodeSourceType The source type indicating where the node originates from
 * @property partiallyExpand Whether the bottom sheet should start in a partially expanded state
 * @property isOnline
 * @property node
 * @property actions
 * @property error
 * @property nodeNameCollisionsResult
 * @property showForeignNodeDialog
 * @property showQuotaDialog
 * @property contactsData
 * @property downloadEvent
 */
data class NodeBottomSheetState(
    val nodeId: Long,
    val nodeSourceType: NodeSourceType,
    val partiallyExpand: Boolean,
    val isOnline: Boolean = false,
    val node: NodeUiItem<TypedNode>? = null,
    val actions: List<List<NodeActionModeMenuItem>> = persistentListOf(),
    val error: StateEventWithContent<Throwable> = consumed(),
    val nodeNameCollisionsResult: StateEventWithContent<NodeNameCollisionsResult> = consumed(),
    val showForeignNodeDialog: StateEvent = consumed,
    val showQuotaDialog: StateEventWithContent<Boolean> = consumed(),
    val contactsData: StateEventWithContent<Pair<List<String>, Boolean>> = consumed(),
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
)