package mega.privacy.android.feature.clouddrive.presentation.clouddrive.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode

data class CloudDriveUiState(
    val title: LocalizedText = LocalizedText.Literal(""),
    val isLoading: Boolean = true,
    val currentFolderId: NodeId = NodeId(-1L),
    val items: List<NodeUiItem<TypedNode>> = emptyList(),
    val navigateToFolderEvent: StateEventWithContent<NodeId> = consumed(),
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