package mega.privacy.android.app.presentation.clouddrive.model

import androidx.annotation.StringRes
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.clouddrive.CloudDriveTab
import mega.privacy.android.core.nodecomponents.components.banners.StorageOverQuotaCapacity.DEFAULT
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.core.nodecomponents.components.banners.StorageOverQuotaCapacity
import mega.privacy.android.app.presentation.validator.toolbaractions.model.SelectedNode
import mega.privacy.android.app.presentation.validator.toolbaractions.modifier.ToolbarActionsModifierItem
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

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
 * @property sourceNodesList list of [NodeUIItem]
 * @property isInSelection if list is in selection mode or not
 * @property selectedNodes selected nodes.
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
 * @property errorMessage The [StringRes] of the message to display in the error banner
 * @property hasNoOpenedFolders true if there are no opened folders
 * @property accountType the account detail
 * @property isHiddenNodesOnboarded if is hidden nodes onboarded
 * @property storageCapacity the storage capacity
 * @property isBusinessAccountExpired if the business or pro flexi is expired
 * @property hiddenNodeEnabled if hidden node is enabled
 * @property isSyncFolderOpen Indicates if the node to open is from Sync Folders. False by default.
 * @property toolbarActionsModifierItem representing the available toolbar actions for the screen.
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
    val sourceNodesList: List<NodeUIItem<TypedNode>> = emptyList(),
    val isInSelection: Boolean = false,
    val selectedNodes: List<SelectedNode> = emptyList(),
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
    @StringRes val errorMessage: Int? = null,
    val accountType: AccountType? = null,
    val isHiddenNodesOnboarded: Boolean = false,
    val storageCapacity: StorageOverQuotaCapacity = DEFAULT,
    val isBusinessAccountExpired: Boolean = false,
    val hiddenNodeEnabled: Boolean = false,
    val isSyncFolderOpen: Boolean = false,
    val isRootNode: Boolean = false,
    val selectedTab: CloudDriveTab = CloudDriveTab.NONE,
    val isFromSyncTab: Boolean = false,
    val showSyncSettings: Boolean = false,
    val showColoredFoldersOnboarding: Boolean = false,
    val toolbarActionsModifierItem: ToolbarActionsModifierItem.CloudDriveSyncs? = null,
) {
    val hasNoOpenedFolders get() = openedFolderNodeHandles.isEmpty()
}
