package mega.privacy.android.app.presentation.shares.incoming.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * IncomingSharesCompose UI state
 *
 * @property currentViewType serves as the original view type
 * @property currentHandle current incoming shares or file browser handle
 * @property currentNodeName current incoming node's name
 * @property isLoading true if loading
 * @property accessedFolderHandle The Folder Handle set when Cloud Drive is opened and
 * there's a Node that immediately needs to be opened
 * @property isAccessedFolderExited true if the User has left the Folder specified by
 * [accessedFolderHandle]
 * @property isPendingRefresh
 * @property nodesList list of [NodeUIItem]
 * @property isInSelection if list is in selection mode or not
 * @property selectedNodes Set of selected node
 * @property totalSelectedFileNodes List of selected node handles
 * @property selectedNodeHandles number of selected file [NodeUIItem] on Compose
 * @property totalSelectedFolderNodes number of selected folder [NodeUIItem] on Compose
 * @property sortOrder [SortOrder] of current list
 * @property optionsItemInfo information when option selected clicked
 * @property isConnected is connected to internet
 * @property downloadEvent download event
 * @property updateToolbarTitleEvent State Event that refreshes the Toolbar Title
 * @property exitIncomingSharesEvent State Event that exits the Incoming Shares page
 * @property openedFolderNodeHandles Set of opened folder node handles to retain scroll position
 * @property isInRootLevel true if in root of incoming shares
 * @property isIncomingSharesEmpty true if there's no incoming shares
 * @property isContactVerificationOn true if contact verification is on
 * @property showContactNotVerifiedBanner true if show contact not verified banner
 * @property showConfirmLeaveShareEvent State Event that shows the Leave Share confirmation dialog
 */
data class IncomingSharesState(
    val currentViewType: ViewType = ViewType.LIST,
    val isLoading: Boolean = true,
    val currentHandle: Long = -1L,
    val currentNodeName: String? = null,
    val isCurrentNodeDecrypted: Boolean? = true,
    val accessedFolderHandle: Long? = null,
    val isAccessedFolderExited: Boolean = false,
    val isPendingRefresh: Boolean = false,
    val nodesList: List<NodeUIItem<ShareNode>> = emptyList(),
    val isInSelection: Boolean = false,
    val selectedNodes: Set<ShareNode> = emptySet(),
    val totalSelectedFileNodes: Int = 0,
    val totalSelectedFolderNodes: Int = 0,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val optionsItemInfo: OptionsItemInfo? = null,
    val isConnected: Boolean = false,
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val updateToolbarTitleEvent: StateEvent = consumed,
    val exitIncomingSharesEvent: StateEvent = consumed,
    val openedFolderNodeHandles: List<Long> = emptyList(),
    val isContactVerificationOn: Boolean = false,
    val showContactNotVerifiedBanner: Boolean = false,
    val showConfirmLeaveShareEvent: StateEventWithContent<List<Long>> = consumed(),
) {
    val isIncomingSharesEmpty: Boolean get() = currentHandle == -1L && nodesList.isEmpty()
    val isInRootLevel: Boolean = currentHandle == -1L
    val selectedNodeHandles: List<Long> = selectedNodes.map { it.id.longValue }
}