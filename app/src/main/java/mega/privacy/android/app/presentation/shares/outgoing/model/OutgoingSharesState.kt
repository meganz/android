package mega.privacy.android.app.presentation.shares.outgoing.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.validator.toolbaractions.model.SelectedNode
import mega.privacy.android.app.presentation.validator.toolbaractions.modifier.ToolbarActionsModifierItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * OutgoingSharesCompose UI state
 *
 * @property currentViewType serves as the original view type
 * @property currentHandle current outgoing shares or file browser handle
 * @property isLoading true if loading
 * @property accessedFolderHandle The Folder Handle set when Cloud Drive is opened and
 * there's a Node that immediately needs to be opened
 * @property isAccessedFolderExited true if the User has left the Folder specified by
 * [accessedFolderHandle]
 * @property isPendingRefresh
 * @property nodesList list of [NodeUIItem]
 * @property isInSelection if list is in selection mode or not
 * @property itemIndex index of item clicked
 * @property currentNodeName current node name
 * @property selectedNodes Set of selected node
 * @property totalSelectedFileNodes List of selected node handles
 * @property selectedNodeHandles number of selected file [NodeUIItem] on Compose
 * @property totalSelectedFolderNodes number of selected folder [NodeUIItem] on Compose
 * @property sortOrder [SortOrder] of current list
 * @property optionsItemInfo information when option selected clicked
 * @property isConnected is connected to internet
 * @property downloadEvent download event
 * @property updateToolbarTitleEvent State Event that refreshes the Toolbar Title
 * @property exitOutgoingSharesEvent State Event that exits the Outgoing Shares page
 * @property openedFolderNodeHandles Set of opened folder node handles to retain scroll position
 * @property isInRootLevel true if in root of outgoing shares
 * @property isOutgoingSharesEmpty true if there's no outgoing shares
 * @property verifyContactDialog dialog to show when clicked on unverified share, contains email
 * @property openAuthenticityCredentials State Event that opens the Authenticity Credentials
 * @property isContactVerificationOn Whether contact verification is enabled in settings
 */
data class OutgoingSharesState(
    val currentViewType: ViewType = ViewType.LIST,
    val isLoading: Boolean = true,
    val currentHandle: Long = -1L,
    val currentNodeName: String? = null,
    val accessedFolderHandle: Long? = null,
    val isAccessedFolderExited: Boolean = false,
    val isPendingRefresh: Boolean = false,
    val nodesList: List<NodeUIItem<ShareNode>> = emptyList(),
    val isInSelection: Boolean = false,
    val selectedNodes: Set<SelectedNode> = emptySet(),
    val totalSelectedFileNodes: Int = 0,
    val totalSelectedFolderNodes: Int = 0,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val optionsItemInfo: OptionsItemInfo? = null,
    val isConnected: Boolean = false,
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val updateToolbarTitleEvent: StateEvent = consumed,
    val exitOutgoingSharesEvent: StateEvent = consumed,
    val openedFolderNodeHandles: Set<Long> = emptySet(),
    val openAuthenticityCredentials: StateEventWithContent<String> = consumed(),
    val verifyContactDialog: String? = null,
    val isContactVerificationOn: Boolean = false,
    val toolbarActionsModifierItem: ToolbarActionsModifierItem.OutgoingShares? = null,
) {
    val isOutgoingSharesEmpty: Boolean = currentHandle == -1L && nodesList.isEmpty()
    val isInRootLevel: Boolean = currentHandle == -1L
    val selectedNodeHandles: List<Long> = selectedNodes.map { it.id }
}
