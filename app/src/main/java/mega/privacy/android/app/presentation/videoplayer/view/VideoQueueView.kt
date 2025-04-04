package mega.privacy.android.app.presentation.videoplayer.view

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.queue.view.MediaQueueItemWithHeaderAndFooterView
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.shared.original.core.ui.controls.lists.DragDropListView
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun VideoQueueView(
    items: List<VideoPlayerItem>,
    currentPlayingPosition: String,
    isPaused: Boolean,
    isSearchMode: Boolean,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    isSelectMode: Boolean = false,
    indexOfDisabledItem: Int = -1,
    onClick: (index: Int, item: VideoPlayerItem) -> Unit = { _, _ -> },
    onDragFinished: () -> Unit = { },
    onMove: (Int, Int) -> Unit = { _, _ -> },
) {
    DragDropListView(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        items = items,
        indexOfDisabledItem = indexOfDisabledItem,
        isDragDropEnabled = !isSearchMode,
        lazyListState = lazyListState,
        onDragFinished = onDragFinished,
        onMove = onMove
    ) { index, item ->
        val isHeaderVisible =
            isSearchMode.not() && ((index == 0 && item.type == MediaQueueItemType.Previous)
                    || item.type == MediaQueueItemType.Playing)
        val isFooterVisible =
            !isSearchMode && (index != items.size - 1 && item.type == MediaQueueItemType.Playing)
        MediaQueueItemWithHeaderAndFooterView(
            icon = item.icon,
            name = item.nodeName,
            currentPlayingPosition = currentPlayingPosition,
            duration = item.duration,
            thumbnailData = ThumbnailRequest(NodeId(item.nodeHandle)),
            isHeaderVisible = isHeaderVisible,
            isFooterVisible = isFooterVisible,
            queueItemType = item.type,
            isAudio = false,
            isPaused = isPaused,
            isSelected = item.isSelected,
            isSearchMode = isSearchMode,
            isSelectMode = isSelectMode,
            onClick = { onClick(index, item) }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MediaQueueViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        VideoQueueView(
            items = initPreviewData(),
            currentPlayingPosition = "00:00",
            isPaused = false,
            lazyListState = LazyListState(),
            onClick = { _, _ -> },
            onMove = { _, _ -> },
            isSearchMode = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MediaQueueViewWithPausedPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        VideoQueueView(
            items = initPreviewData(),
            currentPlayingPosition = "00:00",
            isPaused = true,
            lazyListState = LazyListState(),
            onClick = { _, _ -> },
            onMove = { _, _ -> },
            isSearchMode = false
        )
    }
}

private fun initPreviewData(): List<VideoPlayerItem> =
    (0..6).map {
        if (it == 3) {
            initVideoPlayerItem(it.toLong(), MediaQueueItemType.Playing)
        } else if (it < 3) {
            initVideoPlayerItem(it.toLong(), MediaQueueItemType.Previous)
        } else {
            initVideoPlayerItem(it.toLong(), MediaQueueItemType.Next)
        }
    }

private fun initVideoPlayerItem(
    handle: Long,
    type: MediaQueueItemType,
) =
    VideoPlayerItem(
        icon = iconPackR.drawable.ic_audio_medium_solid,
        nodeHandle = handle,
        nodeName = "Media Name",
        thumbnail = null,
        size = 0,
        type = type,
        duration = "10: 00",
        isSelected = false,
    )