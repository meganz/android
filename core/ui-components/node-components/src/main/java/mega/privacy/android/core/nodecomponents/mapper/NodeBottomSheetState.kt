package mega.privacy.android.core.nodecomponents.mapper

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import mega.privacy.android.core.nodecomponents.model.NodeActionModeMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * Node bottom sheet state
 *
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
    val isOnline: Boolean = false,
    val node: NodeUiItem<TypedNode>? = null,
    val actions: ImmutableList<NodeActionModeMenuItem> = persistentListOf(),
    val error: StateEventWithContent<Throwable> = consumed(),
    val nodeNameCollisionsResult: StateEventWithContent<NodeNameCollisionsResult> = consumed(),
    val showForeignNodeDialog: StateEvent = consumed,
    val showQuotaDialog: StateEventWithContent<Boolean> = consumed(),
    val contactsData: StateEventWithContent<Pair<List<String>, Boolean>> = consumed(),
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
)