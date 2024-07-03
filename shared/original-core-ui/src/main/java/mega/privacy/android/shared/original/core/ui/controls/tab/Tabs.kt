package mega.privacy.android.shared.original.core.ui.controls.tab

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle2medium

/**
 *
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tabs(
    cells: ImmutableList<TextCell>,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState =
        rememberPagerState(initialPage = selectedIndex, initialPageOffsetFraction = 0f) {
            cells.size
        }

    TabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = modifier,
        contentColor = MegaOriginalTheme.colors.border.interactive,
        divider = { },
    ) {
        cells.forEachIndexed { index, cell ->
            TabCell(
                text = cell.text,
                selected = pagerState.currentPage == index,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                modifier = Modifier.testTag(cell.tag)
            )
        }
    }

    HorizontalPager(state = pagerState, modifier = modifier) { page ->
        cells[page].view()
    }
}

/**
 * Tab with Mega style.
 */
@Composable
private fun TabCell(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Tab(
    text = {
        Text(
            text = text,
            color = if (selected) MegaOriginalTheme.colors.components.interactive
            else MegaOriginalTheme.colors.text.secondary,
            style = MaterialTheme.typography.subtitle2medium,
        )
    },
    selected = selected,
    onClick = onClick,
    modifier = modifier,
)

/**
 * Data class for a cell with text.
 *
 * @param text The text to display.
 * @param tag The tag for testing.
 * @param view The view to display.
 */
data class TextCell(
    val text: String,
    val tag: String,
    val view: @Composable () -> Unit,
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkTabsPreview")
@Composable
private fun TabsPreview(
    @PreviewParameter(SelectedTabProvider::class) selectedTab: Int,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        Tabs(
            cells = persistentListOf(
                TextCell("Tab 1", "tab1") {},
                TextCell("Tab 2", "tab2") {},
            ),
            selectedIndex = selectedTab,
        )
    }
}

private class SelectedTabProvider : PreviewParameterProvider<Int> {
    override val values = listOf(0, 1).asSequence()
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkTabCellPreview")
@Composable
private fun TabCellPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        TabCell(
            text = "Tab name",
            selected = false,
            onClick = {},
        )
    }
}