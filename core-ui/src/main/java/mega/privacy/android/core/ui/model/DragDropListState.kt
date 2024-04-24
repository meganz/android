package mega.privacy.android.core.ui.model

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Job

/**
 * State for drag and drop reordering of items in a list.
 *
 * @param lazyListState The state of the list
 * @param onMove The callback is intended to be invoked when an item is moved
 */
internal class DragDropListState(
    val lazyListState: LazyListState,
    private val onMove: (from: Int, to: Int) -> Unit,
) {
    private var draggedDistance by mutableFloatStateOf(0f)

    private var initDraggedLayoutInfo by mutableStateOf<LazyListItemInfo?>(null)

    var draggedItemIndex by mutableStateOf<Int?>(null)

    private val initOffsets: Pair<Int, Int>?
        get() = initDraggedLayoutInfo?.let { Pair(it.offset, it.offsetEnd) }

    val draggingItemOffset: Float?
        get() = draggedItemIndex
            ?.let { lazyListState.getVisibleItemInfo(absoluteIndex = it) }
            ?.let { item ->
                (initDraggedLayoutInfo?.offset ?: 0f).toFloat() + draggedDistance - item.offset
            }

    private val draggedItemLayoutInfo: LazyListItemInfo?
        get() = draggedItemIndex?.let {
            lazyListState.getVisibleItemInfo(absoluteIndex = it)
        }

    private var overscrollJob by mutableStateOf<Job?>(null)

    fun onDragStart(offset: Offset, indexOfDisabledItem: Int = -1) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                item.index > indexOfDisabledItem &&
                        offset.y.toInt() in item.offset..(item.offset + item.size)
            }
            ?.also {
                draggedItemIndex = it.index
                initDraggedLayoutInfo = it
            }
    }

    fun onDragInterrupted() {
        draggedDistance = 0f
        draggedItemIndex = null
        initDraggedLayoutInfo = null
        overscrollJob?.cancel()
    }

    fun onDrag(offset: Offset, indexOfDisabledItem: Int = -1) {
        if (draggedItemIndex != null) {
            draggedDistance += offset.y

            initOffsets?.let { (topOffset, bottomOffset) ->
                val startOffset = topOffset + draggedDistance
                val endOffset = bottomOffset + draggedDistance

                draggedItemLayoutInfo?.let { layoutInfo ->
                    lazyListState.layoutInfo.visibleItemsInfo
                        .filterNot { item ->
                            item.offsetEnd < startOffset || item.offset > endOffset || layoutInfo.index == item.index
                        }
                        .firstOrNull { item ->
                            val draggedOffset = startOffset - layoutInfo.offset
                            when {
                                draggedOffset > 0 -> (endOffset > item.offsetEnd - item.size / 2)
                                else -> (startOffset < item.offset + item.size / 2)
                            }
                        }
                        ?.also { item ->
                            if (item.index > indexOfDisabledItem) {
                                draggedItemIndex?.let { current ->
                                    onMove(current, item.index)
                                }
                                draggedItemIndex = item.index
                            }
                        }
                }
            }
        }
    }

    fun checkForOverScroll(): Float = initDraggedLayoutInfo?.let {
        val startOffset = it.offset + draggedDistance
        val endOffset = it.offsetEnd + draggedDistance

        when {
            draggedDistance > 0 ->
                (endOffset - lazyListState.layoutInfo.viewportEndOffset).takeIf { diff -> diff > 0 }

            draggedDistance < 0 ->
                (startOffset - lazyListState.layoutInfo.viewportStartOffset).takeIf { diff -> diff < 0 }

            else -> null
        }
    } ?: 0f
}

/**
 * LazyListItemInfo.index is the item's absolute index in the list
 * Based on the item's "relative position" with the "currently top" visible item,
 * this returns LazyListItemInfo corresponding to it
 *
 * @param absoluteIndex absolute index of the item
 * @return relative LazyListItemInfo based on current visible items
 */
private fun LazyListState.getVisibleItemInfo(absoluteIndex: Int) =
    layoutInfo.visibleItemsInfo.getOrNull(absoluteIndex - layoutInfo.visibleItemsInfo.first().index)

private val LazyListItemInfo.offsetEnd: Int
    get() = offset + size