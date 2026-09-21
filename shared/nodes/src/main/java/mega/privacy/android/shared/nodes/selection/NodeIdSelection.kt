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

/**
 * A [NodeSelectionState] bound to the ids the user may currently select.
 *
 * The id-only counterpart of [NodeSelection], for screens that never act on the selected nodes
 * themselves and only hand the ids on, such as pickers. Toggling keeps ids selected outside the
 * current selectable set, for example on another tab, and they count towards [selectedCount];
 * [selectAll] replaces the whole selection with the current selectable ids.
 *
 * @property state the underlying selection, exposed for the item composables
 * @property isInSelectionMode true while the user is selecting
 * @property isSelecting true while a select-all is waiting for the remaining pages to load
 * @property selectedNodeIds every selected id
 * @property selectedCount number of selected ids
 * @property isAllSelected true when every selectable id is selected
 */
@Stable
class NodeIdSelection internal constructor(
    val state: NodeSelectionState,
    private val selectableIds: State<Set<NodeId>>,
    private val loadingState: State<NodesLoadingState>,
) {
    val isInSelectionMode: Boolean by derivedStateOf { state.isInSelectionMode }

    val isSelecting: Boolean
        get() = state.selectAllAwaitingMoreItems

    val selectedNodeIds: Set<NodeId>
        get() = state.selectedNodeIds

    val selectedCount: Int
        get() = state.selectedItemsCount

    val isAllSelected: Boolean by derivedStateOf {
        val selectable = selectableIds.value
        selectable.isNotEmpty() && selectable.all { it in state.selectedNodeIds }
    }

    /**
     * Replaces the selection with every selectable id. While the list is still loading, the
     * selection is completed automatically as further pages arrive.
     */
    fun selectAll() {
        state.selectAll(selectableIds.value, loadingState.value)
    }

    fun deselectAll() = state.deselectAll()
}

/**
 * Remembers a [NodeIdSelection] over [selectableNodeIds].
 *
 * @param selectableNodeIds the ids the user may currently select
 * @param loadingState whether the list behind [selectableNodeIds] is complete, so a select-all
 *   can extend to later pages
 * @param state the selection to bind; defaults to one retained for the screen
 */
@Composable
fun rememberNodeIdSelection(
    selectableNodeIds: Set<NodeId>,
    loadingState: NodesLoadingState = NodesLoadingState.FullyLoaded,
    state: NodeSelectionState = rememberNodeSelectionState(),
): NodeIdSelection {
    val currentIds = rememberUpdatedState(selectableNodeIds)
    val currentLoadingState = rememberUpdatedState(loadingState)
    val selection = remember(state) { NodeIdSelection(state, currentIds, currentLoadingState) }

    LaunchedEffect(selection, selectableNodeIds, loadingState) {
        if (state.selectAllAwaitingMoreItems) {
            selection.selectAll()
        }
    }
    return selection
}
