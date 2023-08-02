package mega.privacy.android.app.presentation.photos.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.presentation.photos.model.PhotosTab
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState

/**
 * Main Photos Body View
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosBodyView(
    tabs: List<PhotosTab> = listOf(),
    selectedTab: PhotosTab = PhotosTab.Timeline,
    pagerState: PagerState = rememberPagerState(),
    onTabSelected: (PhotosTab) -> Unit = {},
    timelineView: @Composable () -> Unit = {},
    albumsView: @Composable () -> Unit = {},
    timelineLazyGridState: LazyGridState = LazyGridState(),
    albumsLazyGridState: LazyGridState = LazyGridState(),
    timelineViewState: TimelineViewState = TimelineViewState(),
    albumsViewState: AlbumsViewState = AlbumsViewState(),
) {

    val isBarVisibleTimeline by remember {
        derivedStateOf {
            timelineLazyGridState.firstVisibleItemIndex == 0
        }
    }
    val isScrollingDownTimeline by timelineLazyGridState.isScrollingDown()

    val isBarVisibleAlbums by remember {
        derivedStateOf {
            albumsLazyGridState.firstVisibleItemIndex == 0
        }
    }
    val isScrollingDownAlbums by albumsLazyGridState.isScrollingDown()


    Column(modifier = Modifier.fillMaxSize()) {
        if (timelineViewState.selectedPhotoCount == 0) {
            PhotosTabs(
                tabs = tabs,
                selectedTab = selectedTab,
                isTabSelectionEnabled = albumsViewState.selectedAlbumIds.isEmpty(),
                onTabSelected = onTabSelected,
            ) {
                when (selectedTab) {
                    PhotosTab.Timeline -> isBarVisibleTimeline || !isScrollingDownTimeline
                    PhotosTab.Albums -> isBarVisibleAlbums || !isScrollingDownAlbums
                }
            }
        }
        PagerView(
            tabs = tabs,
            pagerState = pagerState,
            timelineView = timelineView,
            albumsView = albumsView,
            timelineViewState = timelineViewState,
            albumsViewState = albumsViewState,
        )
    }
}

/**
 * The main Tab Row for Timeline-Album
 */
@Composable
fun PhotosTabs(
    modifier: Modifier = Modifier,
    tabs: List<PhotosTab>,
    selectedTab: PhotosTab,
    isTabSelectionEnabled: Boolean,
    onTabSelected: (PhotosTab) -> Unit,
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
            backgroundColor = Color.Transparent,
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == selectedTabIndex,
                    onClick = { onTabSelected(tab) },
                    enabled = isTabSelectionEnabled,
                    text = {
                        Text(
                            text = when (tab) {
                                PhotosTab.Timeline -> stringResource(id = R.string.tab_title_timeline)
                                PhotosTab.Albums -> stringResource(id = R.string.tab_title_album)
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

/**
 * Page content view for Timeline or Album, depending on the selected tab
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerView(
    tabs: List<PhotosTab>,
    pagerState: PagerState,
    timelineView: @Composable () -> Unit,
    albumsView: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    timelineViewState: TimelineViewState,
    albumsViewState: AlbumsViewState,
) {
    HorizontalPager(
        pageCount = tabs.size,
        state = pagerState,
        modifier = modifier,
        userScrollEnabled = timelineViewState.selectedPhotoCount == 0 && albumsViewState.selectedAlbumIds.isEmpty(),
    ) { pageIndex ->
        when (tabs[pageIndex]) {
            PhotosTab.Timeline -> timelineView()
            PhotosTab.Albums -> albumsView()
        }
    }

}

@Composable
internal fun LazyGridState.isScrollingDown(): State<Boolean> {
    var nextIndex by remember(this) { mutableStateOf(firstVisibleItemIndex) }
    var nextScrollOffset by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }
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
