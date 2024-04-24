package mega.privacy.android.app.mediaplayer.queue.view

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemUiEntity
import mega.privacy.android.core.ui.controls.lists.DragDropListView
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.theme.MegaAppTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun MediaQueueView(
    items: List<MediaQueueItemUiEntity>,
    currentPlayingPosition: String,
    isAudio: Boolean,
    isPaused: Boolean,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    indexOfDisabledItem: Int = -1,
    onClick: (index: Int, item: MediaQueueItemUiEntity) -> Unit = { _, _ -> },
    onDragFinished: () -> Unit = { },
    onMove: (Int, Int) -> Unit = { _, _ -> },
) {
    DragDropListView(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        items = items,
        indexOfDisabledItem = indexOfDisabledItem,
        lazyListState = lazyListState,
        onDragFinished = onDragFinished,
        onMove = onMove
    ) { index, item ->
        MediaQueueItemWithHeaderAndFooterView(
            icon = item.icon,
            name = item.nodeName,
            currentPlayingPosition = currentPlayingPosition,
            duration = item.duration,
            thumbnailData = item.thumbnail,
            isHeaderVisible = (index == 0 && item.type == MediaQueueItemType.Previous)
                    || item.type == MediaQueueItemType.Playing,
            isFooterVisible = index != items.size - 1 && item.type == MediaQueueItemType.Playing,
            queueItemType = item.type,
            isAudio = isAudio,
            isPaused = isPaused,
            isSelected = item.isSelected,
            onClick = { onClick(index, item) }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MediaQueueViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        MediaQueueView(
            items = initPreviewData(),
            currentPlayingPosition = "00:00",
            isAudio = true,
            isPaused = false,
            lazyListState = LazyListState(),
            onClick = { _, _ -> },
            onMove = { _, _ -> },
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MediaQueueViewWithPausedPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        MediaQueueView(
            items = initPreviewData(),
            currentPlayingPosition = "00:00",
            isAudio = true,
            isPaused = true,
            lazyListState = LazyListState(),
            onClick = { _, _ -> },
            onMove = { _, _ -> },
        )
    }
}

private fun initPreviewData(): List<MediaQueueItemUiEntity> =
    (0..6).map {
        if (it == 3) {
            initMediaQueueItemUiEntity(it.toLong(), MediaQueueItemType.Playing)
        } else if (it < 3) {
            initMediaQueueItemUiEntity(it.toLong(), MediaQueueItemType.Previous)
        } else {
            initMediaQueueItemUiEntity(it.toLong(), MediaQueueItemType.Next)
        }
    }

private fun initMediaQueueItemUiEntity(
    handle: Long,
    type: MediaQueueItemType,
) =
    MediaQueueItemUiEntity(
        icon = iconPackR.drawable.ic_audio_medium_solid,
        id = NodeId(handle),
        nodeName = "Media Name",
        thumbnail = null,
        type = type,
        duration = "10: 00",
        isSelected = false,
    )