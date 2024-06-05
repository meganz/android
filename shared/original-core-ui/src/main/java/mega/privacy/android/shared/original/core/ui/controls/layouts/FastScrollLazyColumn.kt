package mega.privacy.android.shared.original.core.ui.controls.layouts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
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
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.shared.original.core.ui.controls.other.Counter
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempThemeForPreviews
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * A LazyColumn that shows a vertical scrollbar with a thumb that allows fast scrolling of the list.
 *
 * @param totalItems Total items in the list.
 * @param tooltipText Function to provide the tooltip text for a given index.
 * @param modifier The modifier to be applied to the layout.
 * @param state The state object to be used to control the scrolling of the list.
 * @param contentPadding The padding to be applied to the content.
 * @param reverseLayout Whether the layout should be reversed.
 * @param verticalArrangement The vertical arrangement of the content.
 * @param horizontalAlignment The horizontal alignment of the content.
 * @param flingBehavior The fling behavior of the scrolling.
 * @param userScrollEnabled Whether the user can scroll the list.
 * @param content The content of the list.
 */
@Composable
fun FastScrollLazyColumn(
    totalItems: Int,
    tooltipText: (currentIndex: Int) -> String,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    Box(modifier = modifier) {
        LazyColumn(
            modifier = modifier.testTag(LAZY_COLUMN_TAG),
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            content = content
        )
        VerticalScrollbar(
            tooltipText = tooltipText,
            state = state,
            itemCount = totalItems,
            reverseLayout = reverseLayout,
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun VerticalScrollbar(
    tooltipText: (currentIndex: Int) -> String,
    state: LazyListState,
    itemCount: Int,
    reverseLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val thumbHeightPixels = with(density) { thumbHeight.toPx() }
    // scrollableHeight is the scrollbar height minus thumb height
    var scrollableHeightPixels by remember { mutableFloatStateOf(0f) }
    var scrollableHeight by remember { mutableStateOf(0.dp) }

    var thumbPressed by remember { mutableStateOf(false) }
    // scrollableItemsAmount is item count minus visible items, approximately the first visible item when fully scrolled
    val scrollableItemsAmount by remember {
        derivedStateOf {
            val firstVisibleItemIndex = state.firstVisibleItemIndex
            val lastVisibleItemIndex =
                state.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.toFloat()
                    ?: firstVisibleItemIndex.toFloat()
            val visibleItems = lastVisibleItemIndex - firstVisibleItemIndex
            itemCount - visibleItems - 1
        }
    }
    val thumbOffset by remember {
        derivedStateOf {
            val scrollProportion = if (reverseLayout) {
                1 - state.firstVisibleItemIndex / scrollableItemsAmount
            } else {
                state.firstVisibleItemIndex / scrollableItemsAmount
            }
            scrollableHeight * scrollProportion
        }
    }
    val tooltipString by remember {
        derivedStateOf { tooltipText(state.firstVisibleItemIndex) }
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
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        thumbPressed = false
                    }
                ) { change, _ ->
                    if (thumbPressed && scrollableHeightPixels > 0) {
                        change.consume()
                        val dragProportion = if (reverseLayout) {
                            1 - (change.position.y - thumbHeightPixels / 2) / scrollableHeightPixels
                        } else {
                            (change.position.y - thumbHeightPixels / 2) / scrollableHeightPixels
                        }
                        val targetIndex = (dragProportion * scrollableItemsAmount)
                            .toInt()
                            .coerceIn(0, itemCount - 1)
                        coroutineScope.launch {
                            state.scrollToItem(targetIndex)
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
                visible = state.isScrollInProgress || thumbPressed,
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
                                }
                            )
                        })
            }
            AnimatedVisibility(
                visible = thumbPressed,
                enter = enterAnimation,
                exit = exitAnimation
            ) {
                Counter(tooltipString, modifier = Modifier.offset(x = (-40).dp))
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
        color = MegaOriginalTheme.colors.background.surface1,
        elevation = 8.dp
    ) {
        Icon(
            tint = MegaOriginalTheme.colors.icon.secondary,
            modifier = Modifier
                .padding(8.dp)
                .testTag(THUMB_TAG),
            imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_up_down_small_regular),
            contentDescription = "Chevron up down"
        )
    }

private val thumbHeight = 40.dp
private const val HIDE_DELAY_MILLIS = 900
internal const val THUMB_TAG = "fast_scroll_lazy_column:icon_thumb"
internal const val LAZY_COLUMN_TAG = "fast_scroll_lazy_column:lazy_column_content"

@CombinedThemePreviews
@Composable
private fun FastScrollLazyColumnPreview() {
    OriginalTempThemeForPreviews {
        val items = (0..1000).map { it }
        FastScrollLazyColumn(
            tooltipText = {
                items[it].div(10).times(10).toString()
            },
            modifier = Modifier
                .size(300.dp, 600.dp)
                .background(MegaOriginalTheme.colors.background.pageBackground),
            totalItems = items.size,
        ) {
            itemsIndexed(items) { index, item ->
                MegaText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(MegaOriginalTheme.colors.background.surface1)
                        .padding(8.dp),
                    text = "Text $item", textColor = TextColor.Primary
                )
            }
        }
    }
}


@CombinedThemePreviews
@Composable
private fun FastScrollLazyColumnReversePreview() {
    OriginalTempThemeForPreviews {
        val items = (0..1000).map { it }
        FastScrollLazyColumn(
            horizontalAlignment = Alignment.End,
            reverseLayout = true,
            tooltipText = {
                items[it].div(10).times(10).toString()
            },
            modifier = Modifier
                .size(300.dp, 600.dp)
                .background(MegaOriginalTheme.colors.background.pageBackground),
            totalItems = items.size,
        ) {
            itemsIndexed(items) { index, item ->
                var text by remember { mutableStateOf("$index") }
                OutlinedMegaButton(
                    text, modifier = Modifier
                        .testTag("$index"),
                    rounded = false,
                    onClick = {
                        text = "Clicked"
                    }
                )
            }
        }
    }
}