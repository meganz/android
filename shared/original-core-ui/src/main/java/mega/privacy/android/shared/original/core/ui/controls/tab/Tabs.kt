package mega.privacy.android.shared.original.core.ui.controls.tab

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
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
 * Tabs with Mega style.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tabs(
    modifier: Modifier = Modifier,
    pagerModifier: Modifier = Modifier,
    pagerState: PagerState? = null,
    selectedIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    shouldTabsShown: Boolean = true,
    pagerEnabled: Boolean = true,
    cells: @Composable TabsScope.() -> Unit,
) {
    val activeColor = MegaOriginalTheme.colors.components.interactive
    val color = MegaOriginalTheme.colors.text.secondary
    val tabsScope = TabsScope(activeColor = activeColor, color = color)
    val coroutineScope = rememberCoroutineScope()
    val tabs = with(tabsScope) {
        cells()
        build()
    }
    val pagerState = pagerState ?: rememberPagerState(
        initialPage = selectedIndex,
        initialPageOffsetFraction = 0f
    ) {
        tabs.size
    }
    if (shouldTabsShown) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = modifier,
            contentColor = activeColor,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                        .background(color = MegaOriginalTheme.colors.border.brand)
                )
            }
        ) {
            tabs.forEachIndexed { index, tabItem ->
                tabItem.Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                            onTabSelected(index)
                        }
                    },
                    modifier = Modifier.testTag(tabItem.tag)
                )
            }
        }
    }
    if (pagerEnabled) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            HorizontalPager(
                state = pagerState,
                modifier = pagerModifier,
                userScrollEnabled = shouldTabsShown
            ) { page ->
                Column(modifier = Modifier.fillMaxSize()) {
                    tabs[page].content?.let {
                        tabsScope.it(page == pagerState.currentPage)
                    }
                }
            }
        }
    }


    LaunchedEffect(pagerState.currentPage) {
        onTabSelected(pagerState.currentPage)
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
    icon: @Composable (() -> Unit)? = null,
) = Tab(
    text = {
        Text(
            text = text,
            color = if (selected) MegaOriginalTheme.colors.components.interactive
            else MegaOriginalTheme.colors.text.secondary,
            style = MaterialTheme.typography.subtitle2medium,
        )
    },
    icon = icon,
    selected = selected,
    onClick = onClick,
    modifier = modifier,
)

/**
 * Data class for a Tab with text.
 *
 * @param text The text to display.
 * @param tag The tag for testing.
 * @param content The view to display for the Tab is selected.
 */
private data class TextTabContent(
    val text: String,
    override val tag: String,
    override val content: @Composable (TabsScope.(isActive: Boolean) -> Unit)? = null,
) : TabContent {
    @Composable
    override fun Tab(
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier,
    ) {
        TabCell(
            text = text,
            selected = selected,
            onClick = onClick,
            modifier = modifier,
        )
    }
}

/**
 * Data class for a Tab with text and Icon.
 *
 * @param text The text to display.
 * @param icon The icon to display.
 * @param tag The tag for testing.
 * @param content The view to display.
 */
private data class IconTabContent(
    val text: String,
    val icon: @Composable () -> Unit,
    override val tag: String,
    override val content: @Composable (TabsScope.(isActive: Boolean) -> Unit)? = null,
) : TabContent {
    @Composable
    override fun Tab(
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier,
    ) {
        TabCell(
            text = text,
            icon = icon,
            selected = selected,
            onClick = onClick,
            modifier = modifier,
        )
    }
}

interface TabContent {
    val content: @Composable (TabsScope.(isActive: Boolean) -> Unit)?
    val tag: String

    @Composable
    fun Tab(
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier,
    )
}


/**
 * Scope for building tabs.
 */

class TabsScope(
    private val cells: MutableList<TabContent> = mutableListOf(),
    val activeColor: Color,
    val color: Color,
) {
    fun addTextTab(
        text: String,
        tag: String,
        content: @Composable (TabsScope.(isActive: Boolean) -> Unit)? = null,
    ) {
        cells.add(TextTabContent(text = text, tag = tag, content = content))
    }

    fun addIconTab(
        text: String,
        icon: @Composable () -> Unit,
        tag: String,
        content: @Composable (TabsScope.(isActive: Boolean) -> Unit)? = null,
    ) {
        cells.add(IconTabContent(text = text, icon = icon, tag = tag, content = content))
    }

    internal fun build(): ImmutableList<TabContent> = persistentListOf(*cells.toTypedArray())
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkTabsPreview")
@Composable
private fun TabsPreview(
    @PreviewParameter(SelectedTabProvider::class) selectedTab: Int,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        Tabs(
            cells = {
                addTextTab("Tab 1", "tab1") { Text("Tab 1 content") }
                addTextTab("Tab 2", "tab2") { Text("Tab 2 content") }
            },
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
