package mega.privacy.android.shared.original.core.ui.controls.layouts

import VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalThemeForPreviews

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
    modifier: Modifier = Modifier,
    tooltipText: ((currentIndex: Int) -> String)? = null,
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

internal const val LAZY_COLUMN_TAG = "fast_scroll_lazy_column:lazy_column_content"

@CombinedThemePreviews
@Composable
private fun FastScrollLazyColumnPreview() {
    OriginalThemeForPreviews {
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
    OriginalThemeForPreviews {
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