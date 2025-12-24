package mega.privacy.android.core.nodecomponents.model

import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetResult.RestoreSuccess
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.AddVideoToPlaylistResult
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
 * @property infoToShowEvent Info to show event
 * @property renameNodeRequestEvent Rename node request event
 * @property shareFolderDialogEvent Share folder dialog event
 * @property shareFolderEvent Share folder event
 * @property visibleActions Visible actions
 * @property availableActions Available actions
 * @property navigationEvent Navigation event
 * @property restoreSuccessEvent Restore success event
 * @property dismissEvent Dismiss event
 * @property addVideoToPlaylistResultEvent Add video to playlist result event
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
    val infoToShowEvent: StateEventWithContent<LocalizedText> = consumed(),
    val renameNodeRequestEvent: StateEventWithContent<NodeId> = consumed(),
    val shareFolderDialogEvent: StateEventWithContent<List<Long>> = consumed(),
    val shareFolderEvent: StateEventWithContent<List<Long>> = consumed(),
    val visibleActions: List<MenuActionWithIcon> = emptyList(),
    val availableActions: List<MenuActionWithIcon> = emptyList(),
    val navigationEvent: StateEventWithContent<NavKey> = consumed(),
    val restoreSuccessEvent: StateEventWithContent<RestoreSuccess.RestoreData> = consumed(),
    val dismissEvent: StateEvent = consumed,
    val addVideoToPlaylistResultEvent: StateEventWithContent<AddVideoToPlaylistResult> = consumed(),
    val actionTriggeredEvent: StateEvent = consumed,
)