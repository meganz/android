package mega.privacy.android.app.presentation.videosection

import mega.privacy.android.icon.pack.R as iconPackR
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.UIVideo
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem

@Composable
internal fun AllVideosView(
    items: List<UIVideo>,
    progressBarShowing: Boolean,
    searchMode: Boolean,
    scrollToTop: Boolean,
    lazyListState: LazyListState,
    sortOrder: String,
    modifier: Modifier,
    onClick: (item: UIVideo, index: Int) -> Unit,
    onMenuClick: (UIVideo) -> Unit,
    onSortOrderClick: () -> Unit,
    onLongClick: ((item: UIVideo, index: Int) -> Unit) = { _, _ -> },
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
            text = stringResource(id = R.string.homepage_empty_hint_video),
            imagePainter = painterResource(id = R.drawable.ic_homepage_empty_video)
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
                    val videoItem = items[it]
                    VideoItemView(
                        icon = iconPackR.drawable.ic_video_list,
                        name = videoItem.name,
                        fileSize = formatFileSize(videoItem.size, LocalContext.current),
                        duration = videoItem.duration,
                        isFavourite = videoItem.isFavourite,
                        isSelected = videoItem.isSelected,
                        thumbnailData = if (videoItem.thumbnail?.exists() == true) {
                            videoItem.thumbnail
                        } else {
                            ThumbnailRequest(videoItem.id)
                        },
                        nodeAvailableOffline = videoItem.nodeAvailableOffline,
                        onClick = { onClick(videoItem, it) },
                        onMenuClick = { onMenuClick(videoItem) },
                        onLongClick = { onLongClick(videoItem, it) }
                    )
                }
            }
        }
    }
}