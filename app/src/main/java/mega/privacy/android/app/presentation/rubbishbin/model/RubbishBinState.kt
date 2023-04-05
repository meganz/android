package mega.privacy.android.app.presentation.rubbishbin.model

import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaNode

/**
 *  @property rubbishBinHandle The current RubbishBin Handle
 *  @property nodes List of RubbishBin Nodes
 *  @property parentHandle parent handle of the current node
 *  @property nodeList List of [NodeUIItem]
 *  @property selectedFileNodes number of selected file [NodeUIItem] on Compose
 *  @property selectedFolderNodes number of selected folder [NodeUIItem] on Compose
 *  @property isInSelection to identify if [NodeUIItem] list is in navigation state
 *  @property megaNode [MegaNode]
 *  @property itemIndex index of item clicked
 *  @property currentViewType ViewType The current ViewType used by the UI
 *  @property sortOrder [SortOrder] of current list
 *  @property selectedNodeHandles List of selected node handles
 *  @property selectedMegaNodes List of selected [MegaNode]
 */
data class RubbishBinState(
    val rubbishBinHandle: Long = -1L,
    val nodes: List<MegaNode> = emptyList(),
    val parentHandle: Long? = null,
    val nodeList: List<NodeUIItem> = emptyList(),
    val selectedFileNodes: Int = 0,
    val selectedFolderNodes: Int = 0,
    val isInSelection: Boolean = false,
    val megaNode: MegaNode? = null,
    val itemIndex: Int = -1,
    val currentViewType: ViewType = ViewType.LIST,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val selectedNodeHandles: List<Long> = emptyList(),
    val selectedMegaNodes: List<MegaNode>? = null
)
