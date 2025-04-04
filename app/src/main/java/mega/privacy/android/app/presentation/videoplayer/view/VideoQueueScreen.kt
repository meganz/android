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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import mega.privacy.android.app.mediaplayer.queue.model.VideoQueueMenuAction
import mega.privacy.android.app.mediaplayer.queue.view.VideoQueueTopBar
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModel
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold

@Composable
internal fun VideoQueueScreen(
    viewModel: VideoPlayerViewModel
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val lazyListState = rememberLazyListState()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val systemUiController = rememberSystemUiController()
    var currentPlayingPosition by rememberSaveable { mutableStateOf<String>("") }

    LaunchedEffect(Unit) {
        systemUiController.isSystemBarsVisible = true
        currentPlayingPosition = viewModel.getCurrentPlayingPosition()
    }

    LaunchedEffect(uiState.currentPlayingIndex) {
        if (uiState.currentPlayingIndex != null && uiState.currentPlayingIndex != -1) {
            lazyListState.animateScrollToItem(uiState.currentPlayingIndex)
        }
    }

    val isBackHandlerEnabled =
        uiState.isActionMode || uiState.searchState == SearchWidgetState.EXPANDED

    BackHandler(isBackHandlerEnabled) {
        when {
            uiState.isActionMode -> {
                //Will complete in CC-8676
            }

            uiState.searchState == SearchWidgetState.EXPANDED -> {
                //Will complete in CC-8676
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
                            is VideoQueueMenuAction.VideoQueueSelectAction -> {
                                //Will complete in CC-8676
                            }

                            is VideoQueueMenuAction.VideoQueueRemoveAction -> {
                                //Will complete in CC-8676
                            }
                        }
                    }
                },
                onSearchTextChange = {
                    //Will complete in CC-8676
                },
                onCloseClicked = {
                    //Will complete in CC-8676
                },
                onSearchClicked = {
                    //Will complete in CC-8676
                },
                onBackPressed = {
                    when {
                        uiState.isActionMode -> {
                            //Will complete in CC-8676
                        }

                        uiState.searchState == SearchWidgetState.EXPANDED -> {
                            //Will complete in CC-8676
                        }

                        else ->
                            onBackPressedDispatcher?.onBackPressed()
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
                items = uiState.items,
                currentPlayingPosition = currentPlayingPosition,
                isPaused = true,
                isSearchMode = uiState.searchState == SearchWidgetState.EXPANDED,
                lazyListState = lazyListState,
                onClick = { index, item ->
                    //Will complete in CC-8676
                },
                onDragFinished = {
                    //Will complete in CC-8676
                },
                onMove = { from, to ->
                    //Will complete in CC-8676
                }
            )
        }
    }
}