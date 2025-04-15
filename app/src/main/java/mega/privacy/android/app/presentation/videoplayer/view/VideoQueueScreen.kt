package mega.privacy.android.app.presentation.videoplayer.view

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.queue.model.VideoQueueMenuAction
import mega.privacy.android.app.mediaplayer.queue.view.VideoQueueTopBar
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModel
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold

@Composable
internal fun VideoQueueScreen(
    navHostController: NavHostController,
    viewModel: VideoPlayerViewModel,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val systemUiController = rememberSystemUiController()
    var currentPlayingPosition by rememberSaveable { mutableStateOf<String>("") }

    LaunchedEffect(Unit) {
        systemUiController.isSystemBarsVisible = true
        currentPlayingPosition = viewModel.getCurrentPlayingPosition()
        viewModel.updatePlaybackStateWithReplay(false)
    }

    LaunchedEffect(uiState.currentPlayingIndex) {
        if (uiState.currentPlayingIndex != null && uiState.currentPlayingIndex != -1) {
            lazyListState.animateScrollToItem(uiState.currentPlayingIndex)
        }
    }

    val isBackHandlerEnabled =
        (uiState.isActionMode || uiState.searchState == SearchWidgetState.EXPANDED)

    BackHandler(isBackHandlerEnabled) {
        when {
            uiState.isActionMode -> {
                viewModel.updateActionMode(false)
                viewModel.clearAllSelected()
            }

            uiState.searchState == SearchWidgetState.EXPANDED -> {
                viewModel.closeSearch()
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(uiState.currentPlayingIndex ?: 0)
                }
            }
        }
    }

    MegaScaffold(
        topBar = {
            VideoQueueTopBar(
                title = uiState.playQueueTitle ?: "",
                isActionMode = uiState.isActionMode,
                selectedSize = uiState.selectedItemHandles.size,
                searchState = uiState.searchState,
                query = uiState.query,
                onMenuActionClick = { action ->
                    action?.let {
                        when (it) {
                            is VideoQueueMenuAction.VideoQueueSelectAction ->
                                viewModel.updateActionMode(true)

                            is VideoQueueMenuAction.VideoQueueRemoveAction -> {
                                viewModel.removeSelectedItems()
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

                        uiState.searchState == SearchWidgetState.EXPANDED -> viewModel.closeSearch()
                        else -> onBackPressedDispatcher?.onBackPressed()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            VideoQueueView(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                indexOfDisabledItem = uiState.currentPlayingIndex ?: 0,
                items = if (uiState.searchState == SearchWidgetState.COLLAPSED)
                    uiState.items
                else
                    uiState.searchedItems,
                currentPlayingPosition = currentPlayingPosition,
                isPaused = true,
                isSearchMode = uiState.searchState == SearchWidgetState.EXPANDED,
                lazyListState = lazyListState,
                onClick = { index, item ->
                    coroutineScope.launch {
                        if (uiState.isActionMode) {
                            if (item.type != MediaQueueItemType.Playing) {
                                viewModel.updateItemInSelectionState(index, item)
                            }
                        } else if (!viewModel.isParticipatingInChatCall()) {
                            if (uiState.searchState == SearchWidgetState.EXPANDED) {
                                viewModel.closeSearch()
                            }
                            viewModel.seekToByHandle(item.nodeHandle)
                            navHostController.popBackStack()
                        }
                    }
                },
                onDragFinished = viewModel::updateItemsAfterReorder,
                onMove = viewModel::swapItems
            )
        }
    }
}