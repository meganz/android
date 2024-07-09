package mega.privacy.android.shared.original.core.ui.controls.layouts

import VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempThemeForPreviews
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * A LazyVerticalGrid that shows a vertical scrollbar with a thumb that allows fast scrolling of the list.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param totalItems Total items in the list.
 * @param tooltipText Function to provide the tooltip text for a given index.
 * @param columns The number of columns to be displayed in the grid.
 * @param state The state object to be used to control the scrolling of the list.
 * @param contentPadding The padding to be applied to the content.
 * @param reverseLayout Whether the layout should be reversed.
 * @param verticalArrangement The vertical arrangement of the content.
 * @param horizontalArrangement The horizontal arrangement of the content.
 * @param flingBehavior The fling behavior of the scrolling.
 * @param userScrollEnabled Whether the user can scroll the list.
 * @param content The content of the list.
 */
@Composable
fun FastScrollLazyVerticalGrid(
    totalItems: Int,
    columns: GridCells,
    modifier: Modifier = Modifier,
    tooltipText: ((currentIndex: Int) -> String)? = null,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyGridScope.() -> Unit,
) {
    Box(modifier = modifier) {
        LazyVerticalGrid(
            columns = columns,
            modifier = modifier.testTag(LAZY_GRID_TAG),
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
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

internal const val LAZY_GRID_TAG = "fast_scroll_lazy_grid:lazy_grid_content"

@CombinedThemePreviews
@Composable
private fun FastScrollLazyGridPreview() {
    OriginalTempThemeForPreviews {
        val items = (0..1000).map { it }
        FastScrollLazyVerticalGrid(
            tooltipText = {
                items[it].div(10).times(10).toString()
            },
            modifier = Modifier
                .fillMaxSize()
                .background(MegaOriginalTheme.colors.background.pageBackground),
            totalItems = items.size,
            columns = GridCells.Fixed(1)
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
private fun FastScrollLazyGridReversePreview() {
    OriginalTempThemeForPreviews {
        val items = (0..1000).map { it }
        FastScrollLazyVerticalGrid(
            tooltipText = {
                items[it].div(10).times(10).toString()
            },
            modifier = Modifier
                .fillMaxSize()
                .background(MegaOriginalTheme.colors.background.pageBackground),
            totalItems = items.size,
            columns = GridCells.Fixed(3)
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