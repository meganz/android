package mega.privacy.android.core.ui.controls.lists

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.model.DragDropListState
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.tokens.TextColor

/**
 * A list view that supports drag and drop reordering of items.
 *
 * @param items The list of items to display
 * @param lazyListState The state of the list
 * @param onMove The callback is intended to be invoked when an item is moved, typically to update the list by swapping the dragged item with the target item.
 * @param modifier Modifier
 * @param elevation The elevation of the dragging item, if you don't want elevation, set it to 0.dp
 * @param content the UI of the item, please note that don't add the view that includes the LongPress event, it will cause the event conflict
 */
@Composable
fun <T : Any> DragDropListView(
    items: List<T>,
    lazyListState: LazyListState,
    onMove: (from: Int, to: Int) -> Unit,
    onDragFinished: () -> Unit,
    modifier: Modifier = Modifier,
    indexOfDisabledItem: Int = -1,
    elevation: Dp = 10.dp,
    content: @Composable (index: Int, item: T) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val overscrollJob = remember { mutableStateOf<Job?>(null) }
    val dragDropListState =
        remember { DragDropListState(lazyListState = lazyListState, onMove = onMove) }

    LazyColumn(
        modifier = modifier.setDragGesturesAfterLongPress(
            indexOfDisabledItem = indexOfDisabledItem,
            dragDropListState = dragDropListState,
            scope = scope,
            overscrollJob = overscrollJob,
            onDragFinished = onDragFinished
        ),
        state = dragDropListState.lazyListState
    ) {
        itemsIndexed(items) { index, item ->
            val isDraggingItem = index == dragDropListState.draggedItemIndex
            val offsetOrNull = dragDropListState.draggingItemOffset.takeIf { isDraggingItem }
            Box(
                modifier = Modifier
                    // The zIndex is used to make the dragging item always on top of other items
                    .zIndex(if (isDraggingItem) 1f else 0f)
                    .graphicsLayer {
                        translationY = offsetOrNull ?: 0f
                    }
                    .shadow(elevation = if (isDraggingItem) elevation else 0.dp)
                    .background(MegaTheme.colors.background.pageBackground)
            ) {
                content(index, item)
            }
        }
    }
}

@SuppressLint("ModifierFactoryUnreferencedReceiver")
private fun Modifier.setDragGesturesAfterLongPress(
    dragDropListState: DragDropListState,
    scope: CoroutineScope,
    overscrollJob: MutableState<Job?>,
    onDragFinished: () -> Unit,
    indexOfDisabledItem: Int = -1,
) =
    pointerInput(indexOfDisabledItem) {
        detectDragGesturesAfterLongPress(
            onDrag = { change, offset ->
                dragDropListState.onDrag(offset, indexOfDisabledItem)
                handleOverscrollJob(
                    overscrollJob = overscrollJob,
                    scope = scope,
                    dragDropListState = dragDropListState
                )
                change.consume()
            },
            onDragStart = { offset ->
                dragDropListState.onDragStart(offset, indexOfDisabledItem)
            },
            onDragEnd = {
                dragDropListState.onDragInterrupted()
                onDragFinished()
            },
            onDragCancel = {
                dragDropListState.onDragInterrupted()
            }
        )
    }

private fun handleOverscrollJob(
    overscrollJob: MutableState<Job?>,
    scope: CoroutineScope,
    dragDropListState: DragDropListState,
) {
    if (overscrollJob.value?.isActive == true) return
    val overscrollOffset = dragDropListState.checkForOverScroll()
    if (overscrollOffset != 0f) {
        overscrollJob.value = scope.launch {
            dragDropListState.lazyListState.scrollBy(overscrollOffset)
        }
    } else {
        overscrollJob.value?.cancel()
    }
}

@CombinedThemePreviews
@Composable
private fun DragDropListViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DragDropListView(
            items = listOf("test1", "test2", "test3", "test4", "test5"),
            lazyListState = LazyListState(),
            onDragFinished = {},
            onMove = { _, _ -> }
        ) { _, item ->
            MegaText(modifier = Modifier.fillMaxWidth(), text = item, textColor = TextColor.Primary)
        }
    }
}