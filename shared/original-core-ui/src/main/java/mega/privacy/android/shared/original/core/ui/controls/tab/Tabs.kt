package mega.privacy.android.shared.original.core.ui.controls.tab

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Boolean = { true },
    shouldTabsShown: Boolean = true,
    pagerEnabled: Boolean = false,
    cells: @Composable TabsScope.() -> Unit,
) {
    val activeColor = MegaOriginalTheme.colors.text.brand
    val color = MegaOriginalTheme.colors.text.secondary
    val tabsScope = TabsScope(activeColor = activeColor, color = color)
    val coroutineScope = rememberCoroutineScope()
    val tabs = with(tabsScope) {
        cells()
        build()
    }
    val pagerState = pagerState ?: rememberPagerState(
        initialPage = selectedTabIndex,
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
                        .background(color = activeColor)
                )
            }
        ) {
            tabs.forEachIndexed { index, tabItem ->
                tabItem.Tab(
                    tabsScope = tabsScope,
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            if (onTabSelected(index)) {
                                pagerState.animateScrollToPage(index)
                            }
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
    suffix: @Composable (() -> Unit)? = null,
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
                TabText(text = text, suffix = suffix, selected = selected)
            }
        } ?: run {
            TabText(text = text, suffix = suffix, selected = selected)
        }
    },
    icon = icon,
    selected = selected,
    onClick = onClick,
    modifier = modifier,
)

@Composable
private fun TabText(text: String, suffix: @Composable (() -> Unit)? = null, selected: Boolean) {
    Row {
        Text(
            text = text,
            color = if (selected) MegaOriginalTheme.colors.text.brand
            else MegaOriginalTheme.colors.text.secondary,
            style = MaterialTheme.typography.subtitle2medium,
        )
        suffix?.invoke()
    }
}

/**
 * Data class for a Tab with text.
 *
 * @param text The text to display.
 * @param suffix The view to display after the text.
 * @param badge The badge to display.
 * @param icon The icon to display.
 * @param tag The tag for testing.
 * @param content The view to display for the Tab is selected.
 */
internal data class TabContent(
    val text: String,
    val suffix: @Composable ((activeColor: Color, color: Color) -> Unit)? = null,
    val badge: String? = null,
    val icon: @Composable ((activeColor: Color, color: Color) -> Unit)? = null,
    val tag: String,
    val content: @Composable (TabsScope.(isActive: Boolean) -> Unit)? = null,
) {
    @Composable
    fun Tab(
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
            badge = badge,
            suffix = if (suffix != null) {
                { suffix.invoke(tabsScope.activeColor, tabsScope.color) }
            } else null,
            icon = if (icon != null) {
                { icon.invoke(tabsScope.activeColor, tabsScope.color) }
            } else null
        )
    }
}

/**
 * Scope for building tabs.
 */

class TabsScope(
    val activeColor: Color,
    val color: Color,
) {
    private val cells: MutableList<TabContent> = mutableListOf()
    fun addTextTab(
        text: String,
        tag: String,
        suffix: @Composable ((activeColor: Color, color: Color) -> Unit)? = null,
        badge: String? = null,
        content: @Composable (TabsScope.(isActive: Boolean) -> Unit)? = null,
    ) {
        cells.add(
            TabContent(
                text = text,
                badge = badge,
                suffix = suffix,
                tag = tag,
                content = content,
            )
        )
    }

    fun addIconTab(
        text: String,
        badge: String? = null,
        suffix: @Composable ((activeColor: Color, color: Color) -> Unit)? = null,
        icon: @Composable (activeColor: Color, color: Color) -> Unit,
        tag: String,
        content: @Composable (TabsScope.(isActive: Boolean) -> Unit)? = null,
    ) {
        cells.add(
            TabContent(
                text = text,
                badge = badge,
                icon = icon,
                suffix = suffix,
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
            selectedTabIndex = selectedTab,
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
