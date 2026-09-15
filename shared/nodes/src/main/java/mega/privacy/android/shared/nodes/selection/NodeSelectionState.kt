package mega.privacy.android.shared.nodes.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
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
    private var selectedIds: PersistentSet<NodeId> by mutableStateOf(initialSelectedIds.toPersistentSet())

    val selectedNodeIds: Set<NodeId>
        get() = selectedIds

    var selectAllAwaitingMoreItems: Boolean by mutableStateOf(selectAllInProgress)
        private set

    val isInSelectionMode: Boolean
        get() = selectedNodeIds.isNotEmpty() || selectAllAwaitingMoreItems

    val selectedItemsCount: Int
        get() = selectedNodeIds.size

    fun toggleSelection(nodeId: NodeId) {
        selectedIds = if (nodeId in selectedIds) {
            selectedIds.removing(nodeId)
        } else {
            selectedIds.adding(nodeId)
        }
        if (selectedIds.isEmpty()) {
            selectAllAwaitingMoreItems = false
        }
    }

    fun selectAll(
        nodeIds: Set<NodeId>,
        nodesLoadingState: NodesLoadingState = NodesLoadingState.FullyLoaded,
    ) {
        selectedIds = nodeIds.toPersistentSet()
        selectAllAwaitingMoreItems = !nodesLoadingState.isComplete
    }

    fun deselectAll() {
        selectedIds = persistentSetOf()
        selectAllAwaitingMoreItems = false
    }

    @Deprecated("Use selectAll(nodeIds, nodesLoadingState) instead")
    internal fun startSelecting() {
        selectAllAwaitingMoreItems = true
    }
}

/**
 * Returns a [NodeSelectionState] that is retained across configuration changes and while the
 * hosting navigation entry stays on the back stack. It is not written to saved instance state,
 * so a process death starts with an empty selection.
 *
 * @param initialSelectedIds
 * @param initialIsSelecting
 */
@Composable
fun rememberNodeSelectionState(
    initialSelectedIds: Set<NodeId> = emptySet(),
    initialIsSelecting: Boolean = false,
): NodeSelectionState = retain {
    NodeSelectionState(
        initialSelectedIds = initialSelectedIds,
        selectAllInProgress = initialIsSelecting,
    )
}
