package mega.privacy.android.app.presentation.videoplayer.navigation

import androidx.compose.material.ScaffoldState
import androidx.compose.material.navigation.BottomSheetNavigator
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import mega.privacy.android.app.mediaplayer.videoplayer.navigation.SelectSubtitleScreen
import mega.privacy.android.app.mediaplayer.videoplayer.navigation.VideoQueueScreen
import mega.privacy.android.app.mediaplayer.videoplayer.navigation.selectSubtitleScreen
import mega.privacy.android.app.mediaplayer.videoplayer.navigation.videoQueueScreen
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModel

@Serializable
internal object VideoPlayerRevampNavigationGraph

internal fun NavGraphBuilder.videoPlayerRevampNavigationGraph(
    navHostController: NavHostController,
    bottomSheetNavigator: BottomSheetNavigator,
    scaffoldState: ScaffoldState,
    viewModel: VideoPlayerViewModel,
    player: ExoPlayer?,
    handleAutoReplayIfPaused: () -> Unit,
) {
    navigation<VideoPlayerRevampNavigationGraph>(
        startDestination = VideoPlayerRevampScreenDestination,
    ) {
        videoPlayerRevampScreen(
            navHostController = navHostController,
            bottomSheetNavigator = bottomSheetNavigator,
            scaffoldState = scaffoldState,
            viewModel = viewModel,
            player = player,
            handleAutoReplayIfPaused = handleAutoReplayIfPaused,
        ) {
            navHostController.navigate(VideoQueueScreen)
        }

        videoQueueScreen(
            navHostController = navHostController,
            videoPlayerViewModel = viewModel
        )

        selectSubtitleScreen(
            navHostController = navHostController,
            videoPlayerViewModel = viewModel
        )
    }
}
