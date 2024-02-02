package mega.privacy.android.core.ui.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import mega.privacy.android.core.ui.utils.ListGridStateMap.Companion.Saver
import mega.privacy.android.core.ui.utils.ListStateMap.Companion.Saver

/**
 * The default [Saver] implementation for LazyListState map
 */
class ListStateMap {
    companion object {
        private const val LIST_MAP_ENTRY_SIZE = 3

        /**
         * Custom [Saver] for map of [LazyListState]
         */
        val Saver: Saver<MutableState<Map<Long, LazyListState>>, *> = Saver(
            save = {
                mutableListOf<Any?>().apply {
                    it.value.forEach {
                        add(it.key)
                        add(it.value.firstVisibleItemIndex)
                        add(it.value.firstVisibleItemScrollOffset)
                    }
                }
            },
            restore = { list ->
                val map = mutableMapOf<Long, LazyListState>()
                check(list.size.rem(LIST_MAP_ENTRY_SIZE) == 0)
                var index = 0
                while (index < list.size) {
                    val key = list[index] as Long
                    map[key] = LazyListState(
                        firstVisibleItemIndex = list[index + 1] as Int,
                        firstVisibleItemScrollOffset = list[index + 2] as Int
                    )
                    index += LIST_MAP_ENTRY_SIZE
                }
                mutableStateOf(map.toMap())
            }
        )
    }
}

/**
 * Sync the [LazyListState] map with the opened folder node handles and the current node handle
 */
fun Map<Long, LazyListState>.sync(
    openedHandles: Set<Long>,
    currentHandle: Long,
) = filterKeys { openedHandles.contains(it) || it == currentHandle }
    .toMutableMap()
    .apply {
        if (!containsKey(currentHandle)) {
            this[currentHandle] = LazyListState()
        }
    }

/**
 * Get the [LazyListState] for the given node handle
 */
fun Map<Long, LazyListState>.getState(
    handle: Long,
) = this[handle] ?: LazyListState()