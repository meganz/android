package mega.privacy.android.app.mediaplayer.videoplayer.navigation

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import kotlinx.serialization.Serializable
import mega.privacy.android.app.mediaplayer.videoplayer.view.VideoPlayerScreen
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModel

@Serializable
internal object VideoPlayerScreen

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.videoPlayerScreen(
    navHostController: NavHostController,
    bottomSheetNavigator: BottomSheetNavigator,
    scaffoldState: ScaffoldState,
    viewModel: VideoPlayerViewModel,
    player: ExoPlayer?,
    handleAutoReplayIfPaused: () -> Unit,
    playQueueButtonClicked: () -> Unit,
) {
    composable<VideoPlayerScreen> {
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

        VideoPlayerScreen(
            bottomSheetNavigator = bottomSheetNavigator,
            scaffoldState = scaffoldState,
            viewModel = viewModel,
            player = player,
            playQueueButtonClicked = playQueueButtonClicked
        )
    }
}