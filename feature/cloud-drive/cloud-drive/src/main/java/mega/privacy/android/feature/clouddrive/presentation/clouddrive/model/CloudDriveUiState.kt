package mega.privacy.android.feature.clouddrive.presentation.clouddrive.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * UI state for Cloud Drive
 * @param title Name of the folder
 * @property isLoading True if nodes are loading
 * @property currentFolderId The current folder id being displayed
 * @property items List of nodes in the current folder
 * @property currentViewType The current view type of the Cloud Drive
 * @property navigateToFolderEvent Event to navigate to a folder
 * @property navigateBack Event to navigate back
 * @property openedFileNode The file node that is currently opened
 * @property showHiddenNodes True if hidden nodes should be shown forcefully based on user settings
 * @property isHiddenNodesEnabled True if user is eligible for hidden nodes feature
 * @property isHiddenNodesOnboarded True if the user has been onboarded to hidden nodes feature
 */
data class CloudDriveUiState(
    val title: LocalizedText = LocalizedText.Literal(""),
    val isLoading: Boolean = true,
    val currentFolderId: NodeId = NodeId(-1L),
    val items: List<NodeUiItem<TypedNode>> = emptyList(),
    val currentViewType: ViewType = ViewType.LIST,
    val navigateToFolderEvent: StateEventWithContent<NodeId> = consumed(),
    val navigateBack: StateEvent = consumed,
    val openedFileNode: TypedFileNode? = null,
    val showHiddenNodes: Boolean = false,
    val isHiddenNodesEnabled: Boolean = false,
    val isHiddenNodesOnboarded: Boolean = false,
) {
    /**
     * True if any item is selected
     */
    val isInSelectionMode: Boolean = items.any { it.isSelected }

    /**
     * Returns a list of selected node ids.
     */
    val selectedNodeIds: List<NodeId> get() = items.filter { it.isSelected }.map { it.node.id }
}