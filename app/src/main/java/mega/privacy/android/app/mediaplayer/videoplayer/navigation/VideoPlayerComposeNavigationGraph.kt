package mega.privacy.android.app.mediaplayer.videoplayer.navigation

import androidx.compose.material.ScaffoldState
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModel

@Serializable
internal object VideoPlayerNavigationGraph

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.videoPlayerComposeNavigationGraph(
    navHostController: NavHostController,
    bottomSheetNavigator: BottomSheetNavigator,
    scaffoldState: ScaffoldState,
    viewModel: VideoPlayerViewModel,
    player: ExoPlayer?,
    handleAutoReplayIfPaused: () -> Unit,
) {
    navigation<VideoPlayerNavigationGraph>(
        startDestination = VideoPlayerScreen,
    ) {
        videoPlayerScreen(
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