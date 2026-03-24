package mega.privacy.android.shared.nodes.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState

/**
 * State holder for node multi-selection in the Cloud Drive UI layer.
 *
 * @param initialSelectedIds
 * @param selectAllInProgress
 *
 * @property selectedNodeIds Set of currently selected node IDs
 * @property selectAllAwaitingMoreItems True while a select-all operation is waiting for partial load to finish
 * @property isInSelectionMode True when the user is in selection mode
 * @property selectedItemsCount Number of selected items
 *
 *
 */
@Stable
class NodeSelectionState(
    initialSelectedIds: Set<NodeId> = emptySet(),
    selectAllInProgress: Boolean = false,
) {
    var selectedNodeIds: Set<NodeId> by mutableStateOf(initialSelectedIds)
        private set

    var selectAllAwaitingMoreItems: Boolean by mutableStateOf(selectAllInProgress)
        private set

    val isInSelectionMode: Boolean
        get() = selectedNodeIds.isNotEmpty() || selectAllAwaitingMoreItems

    val selectedItemsCount: Int
        get() = selectedNodeIds.size

    fun toggleSelection(nodeId: NodeId) {
        selectedNodeIds = if (nodeId in selectedNodeIds) {
            val updated = selectedNodeIds - nodeId
            if (updated.isEmpty()) {
                selectAllAwaitingMoreItems = false
            }
            updated
        } else {
            selectedNodeIds + nodeId
        }
    }

    fun selectAll(nodeIds: Set<NodeId>) {
        selectedNodeIds = nodeIds
        selectAllAwaitingMoreItems = false
    }

    fun selectAll(nodeIds: Set<NodeId>, nodesLoadingState: NodesLoadingState) {
        selectedNodeIds = nodeIds
        selectAllAwaitingMoreItems = !nodesLoadingState.isComplete
    }

    fun deselectAll() {
        selectedNodeIds = emptySet()
        selectAllAwaitingMoreItems = false
    }

    @Deprecated("Use selectAll(nodeIds, nodesLoadingState) instead")
    internal fun startSelecting() {
        selectAllAwaitingMoreItems = true
    }

    companion object {
        val Saver: Saver<NodeSelectionState, List<Long>> = Saver(
            save = { state ->
                buildList {
                    add(if (state.selectAllAwaitingMoreItems) 1L else 0L)
                    addAll(state.selectedNodeIds.map { it.longValue })
                }
            },
            restore = { longs ->
                NodeSelectionState(
                    initialSelectedIds = longs.drop(1).map { NodeId(it) }.toSet(),
                    selectAllInProgress = longs.firstOrNull() == 1L
                )
            }
        )
    }
}

/**
 * Remember a [NodeSelectionState] that survives configuration changes.
 *
 * @param initialSelectedIds
 * @param initialIsSelecting
 *
 */
@Composable
fun rememberNodeSelectionState(
    initialSelectedIds: Set<NodeId> = emptySet(),
    initialIsSelecting: Boolean = false,
): NodeSelectionState {
    return rememberSaveable(saver = NodeSelectionState.Saver) {
        NodeSelectionState(
            initialSelectedIds = initialSelectedIds,
            selectAllInProgress = initialIsSelecting
        )
    }
}
