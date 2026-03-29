package mega.privacy.android.feature.clouddrive.presentation.clouddrive.model

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.model.TypedNodeItem

/**
 * Computes the count of selected items, accounting for hidden-node visibility settings.
 *
 * @param selectedIds The set of currently selected node IDs
 * @return The count of selected visible items
 */
fun CloudDriveUiState.computeSelectedItemsCount(selectedIds: Set<NodeId>): Int =
    computeSelectedItemsCount(
        items = items,
        selectedIds = selectedIds,
        showHiddenNodes = showHiddenNodes,
        isHiddenNodesEnabled = isHiddenNodesEnabled,
    )

private fun computeSelectedItemsCount(
    items: List<TypedNodeItem<TypedNode>>,
    selectedIds: Set<NodeId>,
    showHiddenNodes: Boolean,
    isHiddenNodesEnabled: Boolean,
): Int {
    if (showHiddenNodes || !isHiddenNodesEnabled) {
        return items.count { it.node.id in selectedIds }
    }
    return items.count { !it.isSensitive && it.node.id in selectedIds }
}
