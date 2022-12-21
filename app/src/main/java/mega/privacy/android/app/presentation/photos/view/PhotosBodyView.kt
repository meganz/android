@file:OptIn(ExperimentalPagerApi::class)

package mega.privacy.android.app.presentation.photos.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.presentation.photos.model.PhotosTab
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import kotlin.math.roundToInt

/**
 * Main Photos Body View
 */
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

    val toolbarHeight = 50.dp
    val toolbarHeightPx = with(LocalDensity.current) { toolbarHeight.roundToPx().toFloat() }
    val toolbarOffsetHeightPx = remember { mutableStateOf(0f) }
    val contentPadding = remember { mutableStateOf(toolbarOffsetHeightPx.value + toolbarHeightPx) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (
                    timelineLazyGridState.firstVisibleItemScrollOffset != 0 ||
                    albumsLazyGridState.firstVisibleItemScrollOffset != 0
                ) {
                    val delta = available.y
                    val newOffset = toolbarOffsetHeightPx.value + delta
                    toolbarOffsetHeightPx.value = newOffset.coerceIn(-toolbarHeightPx, 0f)
                    contentPadding.value =
                        (toolbarOffsetHeightPx.value + toolbarHeightPx).coerceIn(
                            0F,
                            toolbarHeightPx
                        )
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        if (timelineViewState.selectedPhotoCount == 0) {
            PhotosTabs(
                modifier = Modifier
                    .height(height = toolbarHeight)
                    .offset { IntOffset(x = 0, y = toolbarOffsetHeightPx.value.roundToInt()) },
                tabs = tabs,
                selectedTab = selectedTab,
                isTabSelectionEnabled = albumsViewState.selectedAlbumIds.isEmpty(),
                onTabSelected = onTabSelected,
            )
        }
        PagerView(
            modifier = Modifier
                .padding(top = with(LocalDensity.current) { contentPadding.value.toDp() }),
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
) {
    val selectedTabIndex = selectedTab.ordinal
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

/**
 * Page content view for Timeline or Album, depending on the selected tab
 */
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
        count = tabs.size,
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
