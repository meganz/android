package mega.privacy.android.feature.clouddrive.presentation.rubbishbin.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState

/**
 * M3 UI State for RubbishBin using new NodeUiItem model
 *
 * @property title The title of the current folder
 * @property currentFolderId The current folder ID
 * @property parentFolderId Parent folder ID of the current node
 * @property items List of [mega.privacy.android.core.nodecomponents.model.NodeUiItem] to display
 * @property currentViewType ViewType The current ViewType used by the UI
 * @property sortConfiguration [mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration] of current list
 * @property isLoading Whether the screen is loading
 * @property nodesLoadingState Current state of node loading
 * @property accountType Current account type
 * @property isBusinessAccountExpired Whether business account is expired
 * @property isHiddenNodesEnabled Whether hidden nodes feature is enabled
 * @property isRootDirectory Whether current folder is root rubbish bin
 * @property messageEvent Event to show a message
 * @property openedFileNode Opened file node for file handling
 */
data class NewRubbishBinUiState(
    val title: LocalizedText = LocalizedText.Literal(""),
    val currentFolderId: NodeId = NodeId(-1L),
    val parentFolderId: NodeId? = null,
    val items: List<NodeUiItem<TypedNode>> = emptyList(),
    val currentViewType: ViewType = ViewType.LIST,
    val sortConfiguration: NodeSortConfiguration = NodeSortConfiguration.Companion.default,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val nodesLoadingState: NodesLoadingState = NodesLoadingState.Loading,
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
    val isHiddenNodesEnabled: Boolean = false,
    val isSelecting: Boolean = false,
    val messageEvent: StateEventWithContent<LocalizedText> = consumed(),
    val openedFileNode: TypedFileNode? = null,
    val openFolderEvent: StateEventWithContent<NodeId> = consumed(),
) {
    val isLoading = nodesLoadingState == NodesLoadingState.Loading

    /**
     * Whether current folder is root rubbish bin directory
     */
    val isRootDirectory: Boolean
        get() = currentFolderId.longValue == -1L

    /**
     * Whether the UI is in selection mode
     */
    val isInSelectionMode: Boolean
        get() = items.any { it.isSelected }

    /**
     * Selected nodes
     */
    val selectedNodes: List<TypedNode>
        get() = items.filter { it.isSelected }.map { it.node }

    /**
     * Number of selected file nodes
     */
    val selectedFileNodes: Int
        get() = selectedNodes.count { it is FileNode }

    /**
     * Number of selected folder nodes
     */
    val selectedFolderNodes: Int
        get() = selectedNodes.count { it is FolderNode }
}