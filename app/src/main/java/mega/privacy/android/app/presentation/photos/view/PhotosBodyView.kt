@file:OptIn(ExperimentalPagerApi::class)

package mega.privacy.android.app.presentation.photos.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.PhotosTab
import mega.privacy.android.app.presentation.photos.model.PhotosViewState
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState

@Composable
fun PhotosBodyView(
    photosViewState: PhotosViewState = PhotosViewState(),
    pagerState: PagerState = rememberPagerState(),
    onTabSelected: (PhotosTab) -> Unit = {},
    timelineView: @Composable () -> Unit = {},
    albumsView: @Composable () -> Unit = {},
    timelineViewState: TimelineViewState = TimelineViewState(),
) {
    Column {
        if (timelineViewState.selectedPhotoCount == 0) {
            PhotosTabs(
                tabs = photosViewState.tabs,
                onTabSelected = onTabSelected,
                pagerState = pagerState,
            )
        }
        PagerView(
            tabs = photosViewState.tabs,
            pagerState = pagerState,
            timelineView = timelineView,
            albumsView = albumsView,
            timelineViewState = timelineViewState,
        )
    }
}

@Composable
fun PhotosTabs(
    tabs: List<PhotosTab>,
    onTabSelected: (PhotosTab) -> Unit,
    pagerState: PagerState = rememberPagerState(),
    modifier: Modifier = Modifier,
) {
    val selectedTabIndex = pagerState.currentPage
    TabRow(
        selectedTabIndex = selectedTabIndex,
        indicator = { tabPositions: List<TabPosition> ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = colorResource(id = R.color.red_600_red_300)
            )
        },
        modifier = modifier,
        backgroundColor = Color.Transparent,
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = index == selectedTabIndex,
                onClick = { onTabSelected(tab) },
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

@Composable
fun PagerView(
    tabs: List<PhotosTab>,
    pagerState: PagerState,
    timelineView: @Composable () -> Unit,
    albumsView: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    timelineViewState: TimelineViewState,
) {
    HorizontalPager(
        count = tabs.size,
        state = pagerState,
        modifier = modifier,
        userScrollEnabled = timelineViewState.selectedPhotoCount == 0,
    ) { pageIndex ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            when (tabs[pageIndex]) {
                PhotosTab.Timeline -> timelineView()
                PhotosTab.Albums -> albumsView()
            }
        }
    }
}
