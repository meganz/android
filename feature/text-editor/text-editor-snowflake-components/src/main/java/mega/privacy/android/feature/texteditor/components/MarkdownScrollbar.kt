package mega.privacy.android.feature.texteditor.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.android.core.ui.tokens.theme.DSTokens

private val ThumbSize = 40.dp

/**
 * Draggable scrollbar for the virtualized Markdown [androidx.compose.foundation.lazy.LazyColumn],
 * styled like the editor's fast-scroll thumb (circular surface + handle).
 *
 * Markdown blocks vary hugely in height, so an item-index proportion (like the chunked editor's
 * scrollbar) tracks poorly. Instead this estimates the content's pixel geometry from the measured
 * heights of items seen so far (cached in an [IntArray], averaging unseen items), so the thumb
 * stays close to the true scroll position and refines as more of the document scrolls into view.
 * Dragging maps the thumb fraction back to an item + offset via the same cumulative heights.
 */
@Composable
internal fun MarkdownScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
) {
    val info = state.layoutInfo
    val itemCount = info.totalItemsCount
    if (itemCount == 0 || info.visibleItemsInfo.isEmpty()) return

    // Measured item heights in px (-1 = not yet measured). Reading state.layoutInfo above makes
    // this composable recompose on every scroll, so the array is refreshed each frame.
    val heights = remember(itemCount) { IntArray(itemCount) { -1 } }
    info.visibleItemsInfo.forEach { if (it.index in 0 until itemCount) heights[it.index] = it.size }
    val averageHeight = info.visibleItemsInfo.sumOf { it.size }.toFloat() / info.visibleItemsInfo.size

    val estimatedTotal = estimateTotalHeight(heights, averageHeight)
    val viewport = info.viewportSize.height.toFloat()
    if (estimatedTotal <= viewport) return

    val currentOffset = currentScrollOffset(info, heights, averageHeight)
    val scrollableRange = (estimatedTotal - viewport).coerceAtLeast(1f)
    val fraction = (currentOffset / scrollableRange).coerceIn(0f, 1f)

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val thumbSizePx = with(density) { ThumbSize.toPx() }
    var trackHeightPx by remember { mutableFloatStateOf(0f) }

    // Read latest geometry inside the long-lived drag handler without restarting it.
    val seek by rememberUpdatedState<(Float) -> Unit> { targetFraction ->
        val targetPx = targetFraction * estimatedTotal
        var accumulated = 0f
        var index = 0
        while (index < itemCount - 1 && accumulated + heightAt(index, heights, averageHeight) < targetPx) {
            accumulated += heightAt(index, heights, averageHeight)
            index++
        }
        scope.launch { state.scrollToItem(index, (targetPx - accumulated).toInt().coerceAtLeast(0)) }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(48.dp)
            .onGloballyPositioned { trackHeightPx = it.size.height.toFloat() }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val travel = (trackHeightPx - thumbSizePx).coerceAtLeast(1f)
                    seek(((change.position.y - thumbSizePx / 2f) / travel).coerceIn(0f, 1f))
                }
            },
    ) {
        val travelPx = (trackHeightPx - thumbSizePx).coerceAtLeast(0f)
        val offsetY = with(density) { (fraction * travelPx).toDp() }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = offsetY)
                .padding(end = 8.dp)
                .size(ThumbSize),
            shape = RoundedCornerShape(size = 56.dp),
            color = DSTokens.colors.background.surface1,
            shadowElevation = 8.dp,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        R.drawable.ic_triangle_up_down_small_regular,
                    ),
                    contentDescription = null,
                    tint = DSTokens.colors.icon.secondary,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

private fun heightAt(index: Int, heights: IntArray, average: Float): Float =
    heights[index].let { if (it >= 0) it.toFloat() else average }

private fun estimateTotalHeight(heights: IntArray, average: Float): Float {
    var total = 0f
    for (i in heights.indices) total += heightAt(i, heights, average)
    return total
}

private fun currentScrollOffset(
    info: LazyListLayoutInfo,
    heights: IntArray,
    average: Float,
): Float {
    val first = info.visibleItemsInfo.firstOrNull() ?: return 0f
    var before = 0f
    for (i in 0 until first.index) before += heightAt(i, heights, average)
    return before + (-first.offset).coerceAtLeast(0)
}
