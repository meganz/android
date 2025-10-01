package mega.privacy.android.app.presentation.shares.links.model


import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.validator.toolbaractions.model.SelectedNode
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * Links UI state, associated to the LinksViewModel
 *
 * @property parentNode Parent Node of current Node
 * @property nodesList list of [NodeUIItem]
 * @property isInSelection if list is in selection mode or not
 * @property selectedNodes List of selected nodes
 * @property selectedFileNodes number of selected file [NodeUIItem] on Compose
 * @property selectedFolderNodes number of selected folder [NodeUIItem] on Compose
 * @property sortOrder [SortOrder] of current list
 * @property optionsItemInfo information when option selected clicked
 * @property isConnected is connected to internet
 * @property isLinksEmpty true if links list is empty
 * @property isLoading true if links list is loading
 * @property downloadEvent download event
 * @property updateToolbarTitleEvent State Event that refreshes the Toolbar Title
 * @property exitLinksPageEvent State Event that exits the File Browser
 * @property isInRootLevel true if current page is the first page
 * @property openedFolderNodeHandles Set of opened folder node handles
 * @property currentFolderNodeHandle current folder node handle
 */
data class LinksUiState(
    val parentNode: PublicLinkFolder? = null,
    val nodesList: List<NodeUIItem<PublicLinkNode>> = emptyList(),
    val isInSelection: Boolean = false,
    val selectedNodes: List<SelectedNode> = emptyList(),
    val selectedFileNodes: Int = 0,
    val selectedFolderNodes: Int = 0,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val optionsItemInfo: OptionsItemInfo? = null,
    val isConnected: Boolean = false,
    val isLoading: Boolean = true,
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val updateToolbarTitleEvent: StateEvent = consumed,
    val exitLinksPageEvent: StateEvent = consumed,
    val openedFolderNodeHandles: Set<Long> = emptySet(),
) {
    val isInRootLevel = parentNode == null
    val isLinksEmpty = nodesList.isEmpty() && isInRootLevel && !isLoading
    val currentFolderNodeHandle = parentNode?.id?.longValue ?: -1L
}
