package mega.privacy.android.shared.nodes.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.model.TypedNodeItem

/**
 * A [NodeSelectionState] bound to the items currently on screen.
 *
 * Owns the derivations every multi-select screen needs so they are not re-implemented per screen:
 * which of the visible items are selected, whether all of them are, and a select-all that knows
 * about partially loaded lists. Ids in [state] that no longer match an item are ignored.
 *
 * @param T type of node
 * @property state the underlying selection, exposed for [mega.privacy.android.shared.nodes.components.NodesView]
 * @property isInSelectionMode true while the user is selecting
 * @property isSelecting true while a select-all is waiting for the remaining pages to load
 * @property selectedCount number of selected ids
 * @property selectedNodes the selected nodes among the current items, in item order
 * @property isAllSelected true when every current item is selected
 */
@Stable
class NodeSelection<T : TypedNode> internal constructor(
    val state: NodeSelectionState,
    private val items: State<List<TypedNodeItem<T>>>,
    private val loadingState: State<NodesLoadingState>,
) {
    private val indexById: Map<NodeId, Int> by derivedStateOf {
        buildMap(items.value.size) {
            items.value.forEachIndexed { index, item -> put(item.id, index) }
        }
    }

    val isInSelectionMode: Boolean by derivedStateOf { state.isInSelectionMode }

    val isSelecting: Boolean
        get() = state.selectAllAwaitingMoreItems

    val selectedNodes: List<T> by derivedStateOf {
        val current = items.value
        val index = indexById
        state.selectedNodeIds
            .mapNotNull { index[it] }
            .sorted()
            .map { current[it].node }
    }

    val selectedCount: Int
        get() = state.selectedItemsCount

    val isAllSelected: Boolean by derivedStateOf {
        items.value.isNotEmpty() && selectedNodes.size == items.value.size
    }

    /**
     * Replaces the selection with every current item. While the list is still loading, the
     * selection is completed automatically as further pages arrive.
     */
    fun selectAll() {
        state.selectAll(items.value.mapTo(HashSet()) { it.id }, loadingState.value)
    }

    fun deselectAll() = state.deselectAll()

    fun toggle(nodeId: NodeId) = state.toggleSelection(nodeId)
}

/**
 * Remembers a [NodeSelection] over [items].
 *
 * @param items the selectable items currently shown; pass only the items the user may select
 * @param loadingState whether [items] is complete, so a select-all can extend to later pages
 * @param state the selection to bind; defaults to one retained for the screen
 */
@Composable
fun <T : TypedNode> rememberNodeSelection(
    items: List<TypedNodeItem<T>>,
    loadingState: NodesLoadingState = NodesLoadingState.FullyLoaded,
    state: NodeSelectionState = rememberNodeSelectionState(),
): NodeSelection<T> {
    val currentItems = rememberUpdatedState(items)
    val currentLoadingState = rememberUpdatedState(loadingState)
    val selection = remember(state) { NodeSelection(state, currentItems, currentLoadingState) }

    LaunchedEffect(selection, items, loadingState) {
        if (state.selectAllAwaitingMoreItems) {
            selection.selectAll()
        }
    }
    return selection
}
