package mega.privacy.android.app.mediaplayer.queue.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import mega.privacy.android.app.mediaplayer.queue.audio.AudioQueueViewModel
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemUiEntity

@Composable
internal fun AudioQueueView(
    viewModel: AudioQueueViewModel,
    setupAudioPlayer: (PlayerView) -> Unit,
    onClick: (Int, MediaQueueItemUiEntity) -> Unit,
    onDragFinished: () -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val lazyListState = rememberLazyListState()
    Column(modifier = Modifier.padding(top = 56.dp)) {
        MediaQueueView(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            indexOfDisabledItem = uiState.indexOfCurrentPlayingItem,
            items = uiState.items,
            currentPlayingPosition = uiState.currentPlayingPosition,
            isAudio = true,
            isPaused = uiState.isPaused,
            lazyListState = lazyListState,
            onClick = onClick,
            onDragFinished = onDragFinished,
            onMove = onMove
        )
        SimpleAudioPlayerView(setupAudioPlayer = setupAudioPlayer)
    }

}