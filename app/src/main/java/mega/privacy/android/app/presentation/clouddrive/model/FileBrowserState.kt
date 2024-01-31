package mega.privacy.android.app.presentation.clouddrive.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * File browser UI state
 *
 * @property currentViewType serves as the original view type
 * @property fileBrowserHandle current file browser handle
 * @property isLoading true if loading
 * @property accessedFolderHandle The Folder Handle set when Cloud Drive is opened and
 * there's a Node that immediately needs to be opened
 * @property isAccessedFolderExited true if the User has left the Folder specified by
 * [accessedFolderHandle]
 * @property mediaDiscoveryViewSettings current settings for displaying discovery view
 * @property isPendingRefresh
 * @property nodesList list of [NodeUIItem]
 * @property isInSelection if list is in selection mode or not
 * @property itemIndex index of item clicked
 * @property currentFileNode [FileNode]
 * @property selectedNodeHandles List of selected node handles
 * @property selectedFileNodes number of selected file [NodeUIItem] on Compose
 * @property selectedFolderNodes number of selected folder [NodeUIItem] on Compose
 * @property sortOrder [SortOrder] of current list
 * @property optionsItemInfo information when option selected clicked
 * @property isFileBrowserEmpty information about file browser empty
 * @property shouldShowBannerVisibility
 * @property bannerTime timer
 * @property showMediaDiscoveryIcon showMediaDiscoveryIcon
 * @property isMediaDiscoveryOpen If true, this indicates that Media Discovery is open
 * @property isMediaDiscoveryOpenedByIconClick true if Media Discovery was accessed by clicking the
 * Media Discovery Icon
 * @property isConnected is connected to internet
 * @property downloadEvent download event
 * @property updateToolbarTitleEvent State Event that refreshes the Toolbar Title
 * @property exitFileBrowserEvent State Event that exits the File Browser
 * @property openedFolderNodeHandles Set of opened folder node handles
 */
data class FileBrowserState(
    val currentViewType: ViewType = ViewType.LIST,
    val isLoading: Boolean = true,
    val fileBrowserHandle: Long = -1L,
    val accessedFolderHandle: Long? = null,
    val isAccessedFolderExited: Boolean = false,
    val mediaDiscoveryViewSettings: Int = MediaDiscoveryViewSettings.INITIAL.ordinal,
    val isPendingRefresh: Boolean = false,
    val nodesList: List<NodeUIItem<TypedNode>> = emptyList(),
    val isInSelection: Boolean = false,
    val itemIndex: Int = -1,
    val currentFileNode: FileNode? = null,
    val selectedNodeHandles: List<Long> = emptyList(),
    val selectedFileNodes: Int = 0,
    val selectedFolderNodes: Int = 0,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val optionsItemInfo: OptionsItemInfo? = null,
    val isFileBrowserEmpty: Boolean = false,
    val shouldShowBannerVisibility: Boolean = false,
    val bannerTime: Long = 0L,
    val showMediaDiscoveryIcon: Boolean = false,
    val isMediaDiscoveryOpen: Boolean = false,
    val isMediaDiscoveryOpenedByIconClick: Boolean = false,
    val isConnected: Boolean = false,
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val updateToolbarTitleEvent: StateEvent = consumed,
    val exitFileBrowserEvent: StateEvent = consumed,
    val openedFolderNodeHandles: Set<Long> = emptySet(),
)