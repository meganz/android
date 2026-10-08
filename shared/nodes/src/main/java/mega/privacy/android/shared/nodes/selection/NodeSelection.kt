package mega.privacy.android.shared.nodes.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
 * about partially loaded lists. Once [items] is fully loaded, ids that no longer match an item are
 * dropped from [state], so a node removed while selected stops counting towards the selection and
 * the selected ids are a subset of [items].
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

    internal val selectableIds: Set<NodeId>
        get() = indexById.keys

    val isInSelectionMode: Boolean by derivedStateOf { state.isInSelectionMode }

    val isSelecting: Boolean
        get() = state.selectAllAwaitingMoreItems

    /**
     * Equal counts are not enough: until the list is fully loaded, the selection can still hold an
     * id that is no longer in [items], so the ids are checked whenever the counts match.
     */
    val isAllSelected: Boolean by derivedStateOf {
        val current = items.value
        val selected = state.selectedNodeIds
        current.isNotEmpty() && selected.size == current.size && current.all { it.id in selected }
    }

    val selectedNodes: List<T> by derivedStateOf {
        val current = items.value
        if (isAllSelected) return@derivedStateOf current.map { it.node }
        val index = indexById
        state.selectedNodeIds
            .mapNotNull { index[it] }
            .sorted()
            .map { current[it].node }
    }

    val selectedCount: Int
        get() = state.selectedItemsCount

    /**
     * Replaces the selection with every current item. While the list is still loading, the
     * selection is completed automatically as further pages arrive.
     */
    fun selectAll() {
        state.selectAll(selectableIds, loadingState.value)
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
        } else if (state.isInSelectionMode && loadingState == NodesLoadingState.FullyLoaded) {
            // A node can disappear while selected, e.g. a share revoked by its owner. Only a
            // fully loaded list is evidence of that: a partial page has not arrived yet, and
            // NodesLoadingState.Failed arrives with an empty list.
            state.retainOnly(selection.selectableIds)
        }
    }
    return selection
}

/**
 * Remembers a [NodeSelection] over a paged list. Select-all takes the rows loaded so far; rows that
 * load afterwards are not selected. Ids that are gone after a re-read of the list, a new
 * [reconcileVersion], are dropped.
 *
 * @param items the loaded rows; pass only the items the user may select
 * @param reconcileVersion bumped by the pager after every completed re-read of the loaded rows
 * @param state the selection to bind; defaults to one retained for the screen
 */
@Composable
fun <T : TypedNode> rememberPagedNodeSelection(
    items: List<TypedNodeItem<T>>,
    reconcileVersion: Int,
    state: NodeSelectionState = rememberNodeSelectionState(),
): NodeSelection<T> {
    val currentItems = rememberUpdatedState(items)
    val loadingState = remember { mutableStateOf(NodesLoadingState.FullyLoaded) }
    val selection = remember(state) { NodeSelection(state, currentItems, loadingState) }

    // Keyed on the version, not the list: comparing the lists would walk the whole loaded prefix
    LaunchedEffect(selection, reconcileVersion) {
        if (state.isInSelectionMode) state.retainOnly(selection.selectableIds)
    }
    return selection
}
