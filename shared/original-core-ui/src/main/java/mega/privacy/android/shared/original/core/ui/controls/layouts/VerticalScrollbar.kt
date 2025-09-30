import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.other.Counter

@Composable
internal fun VerticalScrollbar(
    state: LazyListState,
    itemCount: Int,
    reverseLayout: Boolean,
    modifier: Modifier = Modifier,
    tooltipText: ((currentIndex: Int) -> String)? = null,
) = VerticalScrollbar(
    tooltipText = tooltipText,
    state = state,
    firstVisibleItemIndex = remember(state) { derivedStateOf { state.firstVisibleItemIndex } },
    lastVisibleItemIndex = remember(state) {
        derivedStateOf {
            state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: state.firstVisibleItemIndex
        }
    },
    scrollToItem = { state.scrollToItem(it) },
    itemCount = itemCount,
    reverseLayout = reverseLayout,
    modifier = modifier
)


@Composable
internal fun VerticalScrollbar(
    state: LazyGridState,
    itemCount: Int,
    reverseLayout: Boolean,
    modifier: Modifier = Modifier,
    tooltipText: ((currentIndex: Int) -> String)? = null,
) = VerticalScrollbar(
    tooltipText = tooltipText,
    state = state,
    firstVisibleItemIndex = remember(state) { derivedStateOf { state.firstVisibleItemIndex } },
    lastVisibleItemIndex = remember(state) {
        derivedStateOf {
            state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: state.firstVisibleItemIndex
        }
    },
    scrollToItem = { state.scrollToItem(it) },
    itemCount = itemCount,
    reverseLayout = reverseLayout,
    modifier = modifier,

    )

private val thumbHeight = 40.dp
private const val HIDE_DELAY_MILLIS = 900

/**
 * Specifies the minimum item count needed to enable the fast scrollbar thumb.
 * Ensures the scrollbar is hidden when the list contains too few items.
 */
internal object MinimumItemThreshold {
    const val SINGLE_COLUMN = 50
    const val TWO_COLUMN_GRID = 50
    const val MULTI_COLUMN_GRID = 100
}

internal const val THUMB_TAG = "fast_scroll_lazy_column:icon_thumb"

@Composable
private fun VerticalScrollbar(
    state: ScrollableState,
    firstVisibleItemIndex: State<Int>,
    lastVisibleItemIndex: State<Int>,
    scrollToItem: suspend (targetIndex: Int) -> Unit,
    itemCount: Int,
    reverseLayout: Boolean,
    modifier: Modifier = Modifier,
    tooltipText: ((currentIndex: Int) -> String)? = null,
) {

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val thumbHeightPixels = with(density) { thumbHeight.toPx() }
    // scrollableHeight is the scrollbar height minus thumb height
    var scrollableHeightPixels by remember { mutableFloatStateOf(0f) }
    var scrollableHeight by remember { mutableStateOf(0.dp) }

    var thumbPressed by remember { mutableStateOf(false) }
    // scrollableItemsAmount is item count minus visible items, approximately the first visible item when fully scrolled
    val scrollableItemsAmount by remember(
        state,
        itemCount,
        lastVisibleItemIndex.value,
        firstVisibleItemIndex.value
    ) {
        derivedStateOf {
            val visibleItems = lastVisibleItemIndex.value - firstVisibleItemIndex.value
            itemCount - visibleItems - 1
        }
    }

    val thumbOffset by remember(itemCount, state) {
        derivedStateOf {
            val isScrollToEnd =
                lastVisibleItemIndex.value == itemCount - 1 || scrollableItemsAmount == 0
            val scrollProportion = when {
                isScrollToEnd -> if (reverseLayout) 0f else 1f
                reverseLayout -> 1 - firstVisibleItemIndex.value.toFloat() / scrollableItemsAmount
                else -> firstVisibleItemIndex.value.toFloat() / scrollableItemsAmount
            }
            scrollableHeight * scrollProportion
        }
    }

    val thumbVisible by remember(itemCount, state) {
        derivedStateOf {
            itemCount > 0 && state.isScrollInProgress && (state.canScrollForward || state.canScrollBackward) || thumbPressed
        }
    }

    val tooltipString by remember(thumbVisible) {
        derivedStateOf {
            tooltipText?.let { it(firstVisibleItemIndex.value) }
        }
    }
    // Full height box to capture drags
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                with(density) {
                    scrollableHeightPixels = coordinates.size.height.toFloat() - thumbHeight.toPx()
                    scrollableHeight = scrollableHeightPixels.toDp()
                }
            }
            .pointerInput(itemCount, state) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        thumbPressed = false
                    }
                ) { change, _ ->
                    if (thumbPressed && scrollableHeightPixels > 0 && itemCount > 0) {
                        change.consume()

                        val dragPositionY = change.position.y
                        val adjustedY = dragPositionY - thumbHeightPixels / 2

                        val dragProportion = if (reverseLayout) {
                            1 - (adjustedY / scrollableHeightPixels)
                        } else {
                            adjustedY / scrollableHeightPixels
                        }

                        val clampedProportion = dragProportion.coerceIn(0f, 1f)

                        val targetIndex = (clampedProportion * scrollableItemsAmount)
                            .toInt()
                            .coerceIn(0, itemCount - 1)

                        coroutineScope.launch {
                            scrollToItem(targetIndex)
                        }
                    }
                }
            },
    ) {
        // The actual thumb and tooltip
        Box(
            modifier = Modifier
                .height(thumbHeight)
                .offset(y = thumbOffset),
            contentAlignment = Alignment.CenterEnd
        ) {
            val enterAnimation = fadeIn() + scaleIn(
                transformOrigin = TransformOrigin(1f, 0.5f),
                initialScale = 0.5f
            )
            val exitAnimation = scaleOut(
                animationSpec = tween(delayMillis = HIDE_DELAY_MILLIS),
                targetScale = 0.5f,
                transformOrigin = TransformOrigin(1f, 0.5f),
            ) + fadeOut(
                animationSpec = tween(delayMillis = HIDE_DELAY_MILLIS),
            )
            AnimatedVisibility(
                visible = thumbVisible,
                enter = enterAnimation,
                exit = exitAnimation,
            ) {
                Thumb(
                    Modifier
                        .align(Alignment.TopEnd)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    thumbPressed = true
                                    if (tryAwaitRelease()) thumbPressed = false
                                }
                            )
                        })
            }
            tooltipString?.let {
                AnimatedVisibility(
                    visible = thumbPressed,
                    enter = enterAnimation,
                    exit = exitAnimation
                ) {
                    Counter(it, modifier = Modifier.offset(x = (-40).dp))
                }
            }
        }
    }
}

@Composable
private fun Thumb(modifier: Modifier = Modifier) =
    Surface(
        modifier = modifier
            .offset(x = 8.dp)
            .size(thumbHeight),
        shape = CircleShape,
        color = DSTokens.colors.background.surface1,
        elevation = 8.dp
    ) {
        Icon(
            tint = DSTokens.colors.icon.secondary,
            modifier = Modifier
                .padding(8.dp)
                .testTag(THUMB_TAG),
            imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_up_down_small_regular),
            contentDescription = "Chevron up down"
        )
    }