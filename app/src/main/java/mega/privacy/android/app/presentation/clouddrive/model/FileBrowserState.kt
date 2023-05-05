package mega.privacy.android.app.presentation.clouddrive.model

import mega.privacy.android.app.presentation.clouddrive.OptionItems
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaNode

/**
 * File browser UI state
 *
 * @property currentViewType serves as the original view type
 * @property fileBrowserHandle current file browser handle
 * @property mediaDiscoveryViewSettings current settings for displaying discovery view
 * @property nodes List of FileBrowser Nodes
 * @property parentHandle Parent Handle of current Node
 * @property mediaHandle MediaHandle of current Node
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
 */
data class FileBrowserState(
    val currentViewType: ViewType = ViewType.LIST,
    val fileBrowserHandle: Long = -1L,
    val mediaDiscoveryViewSettings: Int = MediaDiscoveryViewSettings.INITIAL.ordinal,
    val nodes: List<MegaNode> = emptyList(),
    val parentHandle: Long? = null,
    val mediaHandle: Long = -1L,
    val isPendingRefresh: Boolean = false,
    val nodesList: List<NodeUIItem> = emptyList(),
    val isInSelection: Boolean = false,
    val itemIndex: Int = -1,
    val currentFileNode: FileNode? = null,
    val selectedNodeHandles: List<Long> = emptyList(),
    val selectedFileNodes: Int = 0,
    val selectedFolderNodes: Int = 0,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val optionsItemInfo: OptionsItemInfo? = null,
)

/**
 * This will hold the necessary information when option item is clicked
 * @property optionClickedType option item type
 * @property selectedMegaNode list selected Mega Node
 * @property selectedNode list of selected Node
 */
data class OptionsItemInfo(
    val optionClickedType: OptionItems,
    val selectedMegaNode: List<MegaNode>,
    val selectedNode: List<Node>,
)
