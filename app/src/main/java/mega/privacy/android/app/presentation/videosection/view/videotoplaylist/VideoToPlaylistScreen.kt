package mega.privacy.android.app.presentation.videosection.view.videotoplaylist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.videosection.VideoToPlaylistViewModel

@Composable
internal fun VideoToPlaylistScreen(
    viewModel: VideoToPlaylistViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    VideoToPlaylistView(
        items = uiState.items,
        searchState = uiState.searchState,
        query = uiState.query,
        hasSelectedItems = uiState.selectedItemIds.isNotEmpty()
    )
}