package mega.privacy.android.feature.videoeditor.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.indicators.InfiniteProgressBarIndicator
import mega.privacy.android.feature.videoeditor.presentation.screen.model.VideoEditorUiState

/**
 * Stateful entry point for the video editor.
 *
 * Hosts the [VideoEditorScreenViewModel] for the given [nodeHandle]
 * @param nodeHandle The MEGA node handle of the video to edit.
 */
@Composable
internal fun VideoEditorRoute(nodeHandle: Long) {
    val viewModel = hiltViewModel<VideoEditorScreenViewModel, VideoEditorScreenViewModel.Factory>(
        creationCallback = { factory -> factory.create(nodeHandle) }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    VideoEditorScreen(uiState = uiState)
}

/**
 * Stateless video editor screen.
 *
 * Shows the download progress while the video is fetched into the cache; the editor UI itself
 * is still a placeholder and will be implemented in a follow-up.
 *
 * @param uiState The current UI state
 * @param modifier Modifier for the composable
 */
@Composable
internal fun VideoEditorScreen(
    uiState: VideoEditorUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading -> InfiniteProgressBarIndicator()

            uiState.isError -> MegaText("Failed to load video")

            // TODO: Implement video editor UI using uiState.videoFilePath
            else -> MegaText("Video Editor")
        }
    }
}
