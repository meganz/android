package mega.privacy.android.app.presentation.videosection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VideoSectionBodyView(
    pagerState: PagerState,
    tabs: List<VideoSectionTab> = listOf(),
    allVideoView: @Composable () -> Unit = {},
    playlistsView: @Composable () -> Unit = {},
    selectedTab: VideoSectionTab = VideoSectionTab.All,
    allLazyListState: LazyListState = LazyListState(),
    playlistsLazyListState: LazyListState = LazyListState(),
    onTabSelected: (VideoSectionTab) -> Unit = {},
) {
    val isBarVisibleAll by remember(allLazyListState) {
        derivedStateOf {
            allLazyListState.firstVisibleItemIndex == 0
        }
    }
    val isScrollingDownAll by allLazyListState.isScrollingDown()
    val isScrollingToEndAll by allLazyListState.isScrolledToEnd()

    val isBarVisiblePlaylists by remember {
        derivedStateOf {
            playlistsLazyListState.firstVisibleItemIndex == 0
        }
    }
    val isScrollingDownPlaylists by playlistsLazyListState.isScrollingDown()
    val isScrollingToEndPlaylists by playlistsLazyListState.isScrolledToEnd()

    Column(modifier = Modifier.fillMaxSize()) {
        VideoSectionTabs(tabs = tabs, selectedTab = selectedTab, onTabSelected = onTabSelected) {
            when (selectedTab) {
                VideoSectionTab.All ->
                    isBarVisibleAll || (!isScrollingDownAll && !isScrollingToEndAll)

                VideoSectionTab.Playlists ->
                    isBarVisiblePlaylists || (!isScrollingDownPlaylists && !isScrollingToEndPlaylists)
            }
        }
        PagerView(
            tabs = tabs,
            pagerState = pagerState,
            allVideoView = allVideoView,
            playlistsView = playlistsView
        )
    }
}

@Composable
internal fun LazyListState.isScrollingDown(): State<Boolean> {
    var nextIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var nextScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (nextIndex != firstVisibleItemIndex) {
                nextIndex < firstVisibleItemIndex
            } else {
                nextScrollOffset <= firstVisibleItemScrollOffset
            }.also {
                nextIndex = firstVisibleItemIndex
                nextScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }
}

@Composable
internal fun LazyListState.isScrolledToEnd() = remember(this) {
    derivedStateOf {
        layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
    }
}

@Composable
internal fun VideoSectionTabs(
    tabs: List<VideoSectionTab>,
    selectedTab: VideoSectionTab,
    onTabSelected: (VideoSectionTab) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: () -> Boolean = { true },
) {
    val selectedTabIndex = selectedTab.ordinal
    AnimatedVisibility(
        visible = isVisible(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        TabRow(
            modifier = modifier,
            selectedTabIndex = selectedTabIndex,
            indicator = { tabPositions: List<TabPosition> ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = colorResource(id = R.color.red_600_red_300)
                )
            },
            backgroundColor = Color.Transparent
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == selectedTabIndex,
                    onClick = { onTabSelected(tab) },
                    enabled = true,
                    text = {
                        Text(
                            text = when (tab) {
                                VideoSectionTab.All -> "All"
                                VideoSectionTab.Playlists -> "Playlists"
                            },
                            fontWeight = FontWeight.Medium
                        )
                    },
                    selectedContentColor = colorResource(id = R.color.red_600_red_300),
                    unselectedContentColor = colorResource(id = R.color.grey_054_white_054)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PagerView(
    tabs: List<VideoSectionTab>,
    pagerState: PagerState,
    allVideoView: @Composable () -> Unit,
    playlistsView: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
    ) { pageIndex ->
        when (tabs[pageIndex]) {
            VideoSectionTab.All -> allVideoView()
            VideoSectionTab.Playlists -> playlistsView()
        }
    }
}