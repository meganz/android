package mega.privacy.android.app.presentation.videosection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VideoSectionComposeView(
    videoSectionViewModel: VideoSectionViewModel,
) {
    val tabState by videoSectionViewModel.tabState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = tabState.selectedTab.ordinal,
        initialPageOffsetFraction = 0f
    ) {
        tabState.tabs.size
    }

    LaunchedEffect(pagerState.currentPage) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            videoSectionViewModel.onTabSelected(selectTab = tabState.tabs[page])
            val tab = VideoSectionTab.values()[page]
            pagerState.scrollToPage(tab.ordinal)
        }
    }

    VideoSectionBodyView(
        pagerState = pagerState,
        tabs = tabState.tabs,
        selectedTab = tabState.selectedTab,
        onTabSelected = { tab ->
            videoSectionViewModel.onTabSelected(selectTab = tab)
            coroutineScope.launch {
                pagerState.scrollToPage(tab.ordinal)
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VideoSectionBodyView(
    pagerState: PagerState,
    tabs: List<VideoSectionTab> = listOf(),
    selectedTab: VideoSectionTab = VideoSectionTab.All,
    onTabSelected: (VideoSectionTab) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        VideoSectionTabs(tabs = tabs, selectedTab = selectedTab, onTabSelected = onTabSelected)
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
                                VideoSectionTab.Playlist -> "Playlist"
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