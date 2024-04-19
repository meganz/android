package mega.privacy.android.app.mediaplayer.queue.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.mediaplayer.queue.audio.AudioQueueViewModel
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemUiEntity

@Composable
internal fun AudioQueueView(
    viewModel: AudioQueueViewModel,
    onClick: (Int, MediaQueueItemUiEntity) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val lazyListState = rememberLazyListState()
    MediaQueueView(
        modifier = Modifier.padding(top = 56.dp),
        items = uiState.items,
        currentPlayingPosition = uiState.currentPlayingPosition,
        isAudio = true,
        isPaused = uiState.isPaused,
        lazyListState = lazyListState,
        onClick = onClick,
        onMove = onMove
    )
}