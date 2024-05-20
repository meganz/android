package mega.privacy.android.app.mediaplayer.queue.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.app.mediaplayer.VideoPlayerViewModel
import mega.privacy.android.app.mediaplayer.queue.video.VideoQueueViewModel

@Composable
internal fun VideoQueueView(
    viewModel: VideoQueueViewModel,
    videoPlayerViewModel: VideoPlayerViewModel,
    onDragFinished: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onToolbarColorUpdated: (Boolean) -> Unit,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LaunchedEffect(uiState.indexOfCurrentPlayingItem) {
        lazyListState.scrollToItem(uiState.indexOfCurrentPlayingItem)
    }

    val isInFirstItem by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex != 0
        }
    }

    LaunchedEffect(isInFirstItem) {
        onToolbarColorUpdated(isInFirstItem)
    }

    Column(modifier = Modifier.padding(top = 80.dp)) {
        MediaQueueView(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            indexOfDisabledItem = uiState.indexOfCurrentPlayingItem,
            items = uiState.items,
            currentPlayingPosition = uiState.currentPlayingPosition,
            isAudio = false,
            isPaused = true,
            isSearchMode = uiState.isSearchMode,
            lazyListState = lazyListState,
            onClick = { _, item ->
                coroutineScope.launch {
                    if (!viewModel.isParticipatingInChatCall()) {
                        videoPlayerViewModel.getIndexFromPlaylistItems(item.id.longValue)
                            ?.let { index ->
                                viewModel.seekTo(index)
                                onBackPressedDispatcher?.onBackPressed()
                            }
                    }
                }
            },
            onDragFinished = onDragFinished,
            onMove = onMove
        )
    }
}