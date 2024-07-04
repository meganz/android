package mega.privacy.android.app.mediaplayer.queue.view

import androidx.activity.compose.BackHandler
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
import mega.privacy.android.app.mediaplayer.LegacyVideoPlayerViewModel
import mega.privacy.android.app.mediaplayer.queue.model.VideoPlayerMenuAction
import mega.privacy.android.app.mediaplayer.queue.video.VideoQueueViewModel
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold

@Composable
internal fun VideoQueueView(
    viewModel: VideoQueueViewModel,
    legacyVideoPlayerViewModel: LegacyVideoPlayerViewModel,
    onDragFinished: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onToolbarColorUpdated: (Boolean) -> Unit,
    onClickedFinished: () -> Unit,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val playlistTitle = legacyVideoPlayerViewModel.playlistTitleState.collectAsStateWithLifecycle().value
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LaunchedEffect(uiState.indexOfCurrentPlayingItem) {
        if (uiState.indexOfCurrentPlayingItem != -1) {
            lazyListState.animateScrollToItem(uiState.indexOfCurrentPlayingItem)
        }
    }

    val isInFirstItem by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex != 0
        }
    }

    LaunchedEffect(isInFirstItem) {
        onToolbarColorUpdated(isInFirstItem)
    }

    val isBackHandlerEnabled =
        uiState.isActionMode || uiState.searchState == SearchWidgetState.EXPANDED

    BackHandler(isBackHandlerEnabled) {
        when {
            uiState.isActionMode -> {
                viewModel.updateActionMode(false)
                viewModel.clearAllSelected()
            }

            uiState.searchState == SearchWidgetState.EXPANDED ->
                viewModel.closeSearch()
        }
    }

    MegaScaffold(
        modifier = Modifier.padding(top = 24.dp),
        topBar = {
            VideoQueueTopBar(
                title = playlistTitle ?: "",
                isActionMode = uiState.isActionMode,
                selectedSize = uiState.selectedItemHandles.size,
                searchState = uiState.searchState,
                query = uiState.query,
                onMenuActionClick = { action ->
                    action?.let {
                        when (it) {
                            is VideoPlayerMenuAction.VideoQueueSelectAction -> {
                                viewModel.updateActionMode(true)
                            }

                            is VideoPlayerMenuAction.VideoQueueRemoveAction -> {
                                viewModel.removeSelectedItems()
                                legacyVideoPlayerViewModel.removeAllSelectedItems()
                                viewModel.updateActionMode(false)
                            }
                        }
                    }
                },
                onSearchTextChange = viewModel::searchQuery,
                onCloseClicked = viewModel::closeSearch,
                onSearchClicked = viewModel::searchWidgetStateUpdate,
                onBackPressed = {
                    when {
                        uiState.isActionMode -> {
                            viewModel.updateActionMode(false)
                            viewModel.clearAllSelected()
                        }

                        uiState.searchState == SearchWidgetState.EXPANDED ->
                            viewModel.closeSearch()

                        else ->
                            onBackPressedDispatcher?.onBackPressed()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            MediaQueueView(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                indexOfDisabledItem = uiState.indexOfCurrentPlayingItem,
                items = uiState.items,
                currentPlayingPosition = uiState.currentPlayingPosition,
                isAudio = false,
                isPaused = true,
                isSearchMode = uiState.searchState == SearchWidgetState.EXPANDED,
                lazyListState = lazyListState,
                onClick = { index, item ->
                    coroutineScope.launch {
                        if (uiState.isActionMode) {
                            viewModel.updateItemInSelectionState(index, item)
                            legacyVideoPlayerViewModel.itemSelected(item.id.longValue)
                            return@launch
                        }
                        if (!viewModel.isParticipatingInChatCall()) {
                            legacyVideoPlayerViewModel.getIndexFromPlaylistItems(item.id.longValue)
                                ?.let { index ->
                                    viewModel.seekTo(index)
                                    onClickedFinished()
                                }
                        }
                    }
                },
                onDragFinished = onDragFinished,
                onMove = onMove
            )
        }
    }
}