package mega.privacy.android.app.presentation.videosection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.UIVideoPlaylist
import mega.privacy.android.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem

@Composable
internal fun VideoPlaylistsView(
    items: List<UIVideoPlaylist>,
    progressBarShowing: Boolean,
    searchMode: Boolean,
    scrollToTop: Boolean,
    lazyListState: LazyListState,
    sortOrder: String,
    modifier: Modifier,
    onClick: (item: UIVideoPlaylist, index: Int) -> Unit,
    onMenuClick: (UIVideoPlaylist) -> Unit,
    onSortOrderClick: () -> Unit,
    onLongClick: ((item: UIVideoPlaylist, index: Int) -> Unit) = { _, _ -> },
) {
    LaunchedEffect(items) {
        if (scrollToTop) {
            lazyListState.scrollToItem(0)
        }
    }

    when {
        progressBarShowing -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp),
                contentAlignment = Alignment.TopCenter,
                content = {
                    MegaCircularProgressIndicator(
                        modifier = Modifier
                            .size(50.dp),
                        strokeWidth = 4.dp,
                    )
                },
            )
        }

        items.isEmpty() -> LegacyMegaEmptyView(
            modifier = modifier,
            text = "[B]No[/B] [A]playlists[/A] [B]found[/B]",
            imagePainter = painterResource(id = R.drawable.ic_homepage_empty_playlists)
        )

        else -> {
            LazyColumn(state = lazyListState, modifier = modifier) {
                if (!searchMode) {
                    item(
                        key = "header"
                    ) {
                        HeaderViewItem(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            onSortOrderClick = onSortOrderClick,
                            onChangeViewTypeClick = {},
                            onEnterMediaDiscoveryClick = {},
                            sortOrder = sortOrder,
                            isListView = true,
                            showSortOrder = true,
                            showChangeViewType = false,
                            showMediaDiscoveryButton = false,
                        )
                    }
                }

                items(count = items.size, key = { items[it].id.longValue }) {
                    val videoPlaylistItem = items[it]
                    VideoPlaylistItemView(
                        icon = R.drawable.ic_playlist_item_empty,
                        title = videoPlaylistItem.title,
                        numberOfVideos = videoPlaylistItem.numberOfVideos,
                        thumbnailList = videoPlaylistItem.thumbnailList,
                        totalDuration = videoPlaylistItem.totalDuration,
                        onClick = { onClick(videoPlaylistItem, it) },
                        onMenuClick = { onMenuClick(videoPlaylistItem) },
                        onLongClick = { onLongClick(videoPlaylistItem, it) }
                    )
                }
            }
        }
    }
}