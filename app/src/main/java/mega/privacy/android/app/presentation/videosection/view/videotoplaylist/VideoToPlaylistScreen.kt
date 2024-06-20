package mega.privacy.android.app.presentation.videosection.view.videotoplaylist

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.videosection.VideoToPlaylistViewModel
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState

@Composable
internal fun VideoToPlaylistScreen(
    viewModel: VideoToPlaylistViewModel,
    addedVideoFinished: (List<String>) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LaunchedEffect(key1 = uiState.addedPlaylistTitles) {
        if (uiState.addedPlaylistTitles.isNotEmpty()) {
            addedVideoFinished(uiState.addedPlaylistTitles)
        }
    }

    BackHandler(uiState.searchState == SearchWidgetState.EXPANDED) {
        viewModel.closeSearch()
    }

    VideoToPlaylistView(
        items = uiState.items,
        isLoading = uiState.isLoading,
        isInputTitleValid = uiState.isInputTitleValid,
        showCreateVideoPlaylistDialog = uiState.shouldCreateVideoPlaylist,
        inputPlaceHolderText = uiState.createVideoPlaylistPlaceholderTitle,
        setShouldCreateVideoPlaylist = viewModel::setShouldCreateVideoPlaylist,
        onCreateDialogPositiveButtonClicked = viewModel::createNewPlaylist,
        setInputValidity = viewModel::setNewPlaylistTitleValidity,
        setDialogInputPlaceholder = viewModel::setPlaceholderTitle,
        searchState = uiState.searchState,
        query = uiState.query,
        hasSelectedItems = uiState.items.any { it.isSelected },
        errorMessage = uiState.createDialogErrorMessage,
        onSearchClicked = viewModel::searchWidgetStateUpdate,
        onSearchTextChange = viewModel::searchQuery,
        onCloseClicked = viewModel::closeSearch,
        onBackPressed = {
            when {
                uiState.searchState == SearchWidgetState.EXPANDED ->
                    viewModel.closeSearch()

                else ->
                    onBackPressedDispatcher?.onBackPressed()
            }
        },
        onItemClicked = viewModel::updateItemInSelectionState,
        onDoneButtonClicked = viewModel::addVideoToMultiplePlaylists
    )
}