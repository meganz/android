package mega.privacy.android.shared.original.core.ui.model


import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * A component to retain scroll position of LazyListState and LazyGridState across navigation
 * Features:
 * - Automatic scroll position retention across navigation, e.g. when navigating back to a folder
 * - Support for both List and Set navigation handle types
 * - Persistent state across configuration changes
 */

/**
 * Creates a [ListGridState] that is remembered across compositions.
 *
 * @param currentHandle Current folder node handle
 * @param navigationHandles Collection of folder node handles that are opened.
 */
@Composable
fun rememberListGridNavigationState(
    currentHandle: Long,
    navigationHandles: Collection<Long>,
): ListGridState {
    val manager = rememberListGridNavigationStateStore(currentHandle, navigationHandles)
    return manager.getOrCreateState(currentHandle)
}

@Composable
private fun rememberListGridNavigationStateStore(
    currentHandle: Long,
    navigationHandles: Collection<Long>,
): ListGridStateStore {
    val manager = rememberSaveable(saver = ListGridStateStore.Saver) {
        ListGridStateStore()
    }
    // Syncs states in store when current handle or navigation handles change
    LaunchedEffect(navigationHandles, currentHandle) {
        manager.syncStates(navigationHandles, currentHandle)
    }
    return manager
}

/**
 * Centralized state store for [ListGridState] instances across navigation levels.
 */
private class ListGridStateStore(
    initialState: Map<Long, ListGridState> = emptyMap<Long, ListGridState>(),
) {
    var stateMap by mutableStateOf(initialState)
        private set

    /**
     * Sync state map based on handles and current handle.
     *
     * @param navigationHandles
     * @param currentHandle
     */
    fun syncStates(navigationHandles: Collection<Long>, currentHandle: Long) {
        val newValidHandles = (navigationHandles + currentHandle)
        val currentHandles = stateMap.keys

        // Only update if the valid handles have actually changed
        if (newValidHandles != currentHandles) {
            // Clean up invalid states (only when necessary)
            stateMap = stateMap.filterKeys { it in newValidHandles }
        }
    }

    /**
     * Retrieves the [ListGridState] for the given handle, or creates a new one if it doesn't exist.
     *
     * @param handle The node handle for which to retrieve or create the state.
     * @return The [ListGridState] associated with the given handle.
     */
    fun getOrCreateState(handle: Long): ListGridState {
        return stateMap[handle] ?: run {
            val newState = ListGridState()
            stateMap = stateMap + (handle to newState)
            newState
        }
    }

    companion object {
        private const val LIST_GRID_MAP_ENTRY_SIZE = 5

        /**
         * Saver for ListGridNavigationState that preserves scroll positions
         */
        val Saver: Saver<ListGridStateStore, *> = Saver(
            save = { manager ->
                mutableListOf<Any?>().apply {
                    manager.stateMap.forEach { (key, state) ->
                        add(key)
                        add(state.lazyListState.firstVisibleItemIndex)
                        add(state.lazyListState.firstVisibleItemScrollOffset)
                        add(state.lazyGridState.firstVisibleItemIndex)
                        add(state.lazyGridState.firstVisibleItemScrollOffset)
                    }
                }
            },
            restore = { list ->
                val map = mutableMapOf<Long, ListGridState>()
                if (list.isNotEmpty()) {
                    if (list.size.rem(LIST_GRID_MAP_ENTRY_SIZE) != 0) {
                        return@Saver ListGridStateStore(emptyMap())
                    }
                    var index = 0
                    while (index < list.size) {
                        val key = list[index] as Long
                        val listState = LazyListState(
                            firstVisibleItemIndex = list[index + 1] as Int,
                            firstVisibleItemScrollOffset = list[index + 2] as Int
                        )
                        val gridState = LazyGridState(
                            firstVisibleItemIndex = list[index + 3] as Int,
                            firstVisibleItemScrollOffset = list[index + 4] as Int
                        )
                        map[key] = ListGridState(listState, gridState)
                        index += LIST_GRID_MAP_ENTRY_SIZE
                    }
                }
                ListGridStateStore(map)
            }
        )
    }
}

