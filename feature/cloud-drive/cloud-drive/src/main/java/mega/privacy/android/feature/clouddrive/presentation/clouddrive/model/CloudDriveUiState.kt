package mega.privacy.android.feature.clouddrive.presentation.clouddrive.model

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * UI state for Cloud Drive
 * @param title Name of the folder
 * @property nodesLoadingState Current state of node loading
 * @property currentFolderId The current folder id being displayed
 * @property isCloudDriveRoot True if the current folder is the root of the Cloud Drive
 * @property items List of nodes in the current folder
 * @property currentViewType The current view type of the Cloud Drive
 * @property navigateToFolderEvent Event to navigate to a folder
 * @property navigateBack Event to navigate back
 * @property openedFileNode The file node that is currently opened
 * @property showHiddenNodes True if hidden nodes should be shown forcefully based on user settings
 * @property isHiddenNodesEnabled True if user is eligible for hidden nodes feature
 * @property isHiddenNodesOnboarded True if the user has been onboarded to hidden nodes feature, show onboarding screen based on it
 * @property isSelecting True if nodes are being selected
 * @property hasMediaItems True if there are media(image, video) items in the current folder
 */
data class CloudDriveUiState(
    val title: LocalizedText = LocalizedText.Literal(""),
    val nodesLoadingState: NodesLoadingState = NodesLoadingState.Loading,
    val isHiddenNodeSettingsLoading: Boolean = true,
    val currentFolderId: NodeId = NodeId(-1L),
    val isCloudDriveRoot: Boolean = false,
    val items: List<NodeUiItem<TypedNode>> = emptyList(),
    val currentViewType: ViewType = ViewType.LIST,
    val navigateToFolderEvent: StateEventWithContent<TypedNode> = consumed(),
    val navigateBack: StateEvent = consumed,
    val openedFileNode: TypedFileNode? = null,
    val showHiddenNodes: Boolean = false,
    val isHiddenNodesEnabled: Boolean = false,
    val isHiddenNodesOnboarded: Boolean = true,
    val gmsDocumentScanner: GmsDocumentScanner? = null,
    val documentScanningError: DocumentScanningError? = null,
    val isSelecting: Boolean = false,
    val hasMediaItems: Boolean = false,
    val selectedSortOrder: SortOrder = SortOrder.ORDER_DEFAULT_ASC,
    val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
) {

    /**
     * True if nodes or hidden node settings are loading
     */
    val isLoading = nodesLoadingState == NodesLoadingState.Loading || isHiddenNodeSettingsLoading

    /**
     * Count of visible items based on hidden nodes settings
     */
    val visibleItemsCount: Int

    /**
     * Count of visible selected items
     */
    val selectedItemsCount: Int

    init {
        // Count visible and selected items based on hidden nodes settings with single loop
        if (showHiddenNodes || !isHiddenNodesEnabled) {
            visibleItemsCount = items.size
            selectedItemsCount = items.count { it.isSelected }
        } else {
            var visible = 0
            var selected = 0
            items.forEach { item ->
                if (!item.isSensitive) {
                    visible++
                    if (item.isSelected) {
                        selected++
                    }
                }
            }
            visibleItemsCount = visible
            selectedItemsCount = selected
        }
    }

    /**
     * True if any item is selected
     */
    val isInSelectionMode = selectedItemsCount > 0

    /**
     * True if there are no visible items and not loading
     */
    val isEmpty = visibleItemsCount == 0 && !isLoading

    /**
     * Returns a list of selected nodes.
     */
    val selectedNodes: List<TypedNode>
        get() = items.mapNotNull { if (it.isSelected) it.node else null }

    /**
     * Returns a list of selected node ids.
     */
    val selectedNodeIds: List<NodeId>
        get() = selectedNodes.map { it.id }
}

/**
 * Sealed interface representing the different states of progressive node loading
 */
sealed interface NodesLoadingState {
    object Loading : NodesLoadingState
    object PartiallyLoaded : NodesLoadingState
    object FullyLoaded : NodesLoadingState
}