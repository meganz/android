package mega.privacy.android.app.presentation.node.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.node.view.BottomSheetMenuItem
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Node bottom sheet state
 *
 * @property name
 * @property isOnline
 * @property node
 * @property actions
 * @property error
 * @property nodeNameCollisionResult
 * @property showForeignNodeDialog
 * @property showQuotaDialog
 * @property accessPermissionIcon
 * @property shareInfo
 * @property outgoingShares
 * @property contactsData
 * @property downloadEvent
 */
data class NodeBottomSheetState(
    val name: String = "",
    val isOnline: Boolean = false,
    val node: TypedNode? = null,
    val actions: List<BottomSheetMenuItem> = emptyList(),
    val error: StateEventWithContent<Throwable> = consumed(),
    val nodeNameCollisionResult: StateEventWithContent<NodeNameCollisionResult> = consumed(),
    val showForeignNodeDialog: StateEvent = consumed,
    val showQuotaDialog: StateEventWithContent<Boolean> = consumed(),
    val accessPermissionIcon: Int? = null,
    val shareInfo: String? = null,
    val outgoingShares: List<ShareData> = emptyList(),
    val contactsData: StateEventWithContent<Pair<List<String>, Boolean>> = consumed(),
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
)