package mega.privacy.android.shared.original.core.ui.controls.tab

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    tabsScope = tabsScope,
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
    badge: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
) = Tab(
    text = {
        badge?.let {
            BadgedBox(
                badge = {
                    Badge(modifier = Modifier.padding(6.dp)) {
                        Text(it, fontSize = 8.sp)
                    }
                },
            ) {
                Text(
                    text = text,
                    color = if (selected) MegaOriginalTheme.colors.components.interactive
                    else MegaOriginalTheme.colors.text.secondary,
                    style = MaterialTheme.typography.subtitle2medium,
                )
            }
        } ?: run {
            Text(
                text = text,
                color = if (selected) MegaOriginalTheme.colors.components.interactive
                else MegaOriginalTheme.colors.text.secondary,
                style = MaterialTheme.typography.subtitle2medium,
            )

        }
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
    override val text: String,
    override val badge: String? = null,
    override val tag: String,
    override val content: @Composable (TabsScope.(isActive: Boolean) -> Unit)? = null,
) : TabContent {
    @Composable
    override fun Tab(
        tabsScope: TabsScope,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier,
    ) {
        TabCell(
            text = text,
            selected = selected,
            onClick = onClick,
            modifier = modifier,
            badge = badge
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
    override val text: String,
    override val badge: String? = null,
    val icon: @Composable (activeColor: Color, color: Color) -> Unit,
    override val tag: String,
    override val content: @Composable (TabsScope.(isActive: Boolean) -> Unit)? = null,
) : TabContent {
    @Composable
    override fun Tab(
        tabsScope: TabsScope,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier,
    ) {
        TabCell(
            text = text,
            icon = { icon(tabsScope.activeColor, tabsScope.color) },
            selected = selected,
            onClick = onClick,
            modifier = modifier,
            badge = badge
        )
    }
}

interface TabContent {
    val text: String
    val badge: String?
    val content: @Composable (TabsScope.(isActive: Boolean) -> Unit)?
    val tag: String

    @Composable
    fun Tab(
        tabsScope: TabsScope,
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
        badge: String? = null,
        content: @Composable (TabsScope.(isActive: Boolean) -> Unit)? = null,
    ) {
        cells.add(TextTabContent(text = text, badge = badge, tag = tag, content = content))
    }

    fun addIconTab(
        text: String,
        badge: String? = null,
        icon: @Composable (activeColor: Color, color: Color) -> Unit,
        tag: String,
        content: @Composable (TabsScope.(isActive: Boolean) -> Unit)? = null,
    ) {
        cells.add(
            IconTabContent(
                text = text,
                badge = badge,
                icon = icon,
                tag = tag,
                content = content
            )
        )
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
