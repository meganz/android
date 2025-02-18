package mega.privacy.android.app.mediaplayer.queue.view

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.app.mediaplayer.queue.audio.AudioQueueViewModel
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemUiEntity
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R

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

    LaunchedEffect(uiState.indexOfCurrentPlayingItem) {
        if (uiState.indexOfCurrentPlayingItem > -1) {
            lazyListState.animateScrollToItem(uiState.indexOfCurrentPlayingItem)
        }
    }

    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    EventEffect(
        event = uiState.itemsRemovedEvent,
        onConsumed = viewModel::onItemsRemovedEventConsumed
    ) {
        coroutineScope.launch {
            scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                if (uiState.removedItems.size == 1)
                    context.resources.getString(
                        R.string.audio_queue_single_item_removed_message,
                        uiState.removedItems.first().nodeName
                    )
                else
                    context.resources.getString(
                        R.string.audio_queue_multiple_items_removed_message,
                        uiState.removedItems.size
                    )
            )
            viewModel.clearRemovedItemHandles()
        }
    }

    MegaScaffold(
        modifier = Modifier.padding(top = 56.dp),
        scaffoldState = scaffoldState,
        bottomBar = {
            SimpleAudioPlayerView(setupAudioPlayer = setupAudioPlayer)
        }
    ) { paddingValue ->
        MediaQueueView(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValue),
            indexOfDisabledItem = uiState.indexOfCurrentPlayingItem,
            items = uiState.items,
            currentPlayingPosition = uiState.currentPlayingPosition,
            isAudio = true,
            isPaused = uiState.isPaused,
            isSearchMode = uiState.isSearchMode,
            isSelectMode = uiState.isSelectMode,
            lazyListState = lazyListState,
            onClick = onClick,
            onDragFinished = onDragFinished,
            onMove = onMove
        )
    }
}