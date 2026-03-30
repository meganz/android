package mega.privacy.android.app.presentation.videoplayer.navigation

import androidx.compose.material.ScaffoldState
import androidx.compose.material.navigation.BottomSheetNavigator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.mediaplayer.videoplayer.navigation.SelectSubtitleScreen
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModel
import mega.privacy.android.app.presentation.videoplayer.view.VideoPlayerRevampScreen

@Serializable
internal object VideoPlayerRevampScreenDestination

internal fun NavGraphBuilder.videoPlayerRevampScreen(
    navHostController: NavHostController,
    bottomSheetNavigator: BottomSheetNavigator,
    scaffoldState: ScaffoldState,
    viewModel: VideoPlayerViewModel,
    player: ExoPlayer?,
    handleAutoReplayIfPaused: () -> Unit,
    playQueueButtonClicked: () -> Unit,
) {
    composable<VideoPlayerRevampScreenDestination> {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) {
            handleAutoReplayIfPaused()
        }

        LaunchedEffect(uiState.navigateToSelectSubtitleScreen) {
            if (uiState.navigateToSelectSubtitleScreen) {
                navHostController.navigate(SelectSubtitleScreen)
                viewModel.updateNavigateToSelectSubtitle(false)
            }
        }

        VideoPlayerRevampScreen(
            bottomSheetNavigator = bottomSheetNavigator,
            scaffoldState = scaffoldState,
            viewModel = viewModel,
            player = player,
            playQueueButtonClicked = playQueueButtonClicked
        )
    }
}
