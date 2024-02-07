package mega.privacy.android.app.presentation.shares.outgoing.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.entity.preference.ViewType

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
 * @property currentFileNode [FileNode]
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
 * @property isInRoot true if in root
 * @property isOutgoingSharesEmpty true if there's no outgoing shares
 */
data class OutgoingSharesState(
    val currentViewType: ViewType = ViewType.LIST,
    val isLoading: Boolean = true,
    val currentHandle: Long = -1L,
    val accessedFolderHandle: Long? = null,
    val isAccessedFolderExited: Boolean = false,
    val isPendingRefresh: Boolean = false,
    val nodesList: List<NodeUIItem<ShareNode>> = emptyList(),
    val isInSelection: Boolean = false,
    val itemIndex: Int = -1,
    val currentFileNode: FileNode? = null,
    val selectedNodes: Set<ShareNode> = emptySet(),
    val totalSelectedFileNodes: Int = 0,
    val totalSelectedFolderNodes: Int = 0,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val optionsItemInfo: OptionsItemInfo? = null,
    val isConnected: Boolean = false,
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val updateToolbarTitleEvent: StateEvent = consumed,
    val exitOutgoingSharesEvent: StateEvent = consumed,
    val openedFolderNodeHandles: Set<Long> = emptySet(),
) {
    val isOutgoingSharesEmpty: Boolean
        get() = currentHandle == -1L && nodesList.isEmpty()

    val isInRoot: Boolean
        get() = currentHandle == -1L

    val selectedNodeHandles: List<Long> get() = selectedNodes.map { it.id.longValue }
}