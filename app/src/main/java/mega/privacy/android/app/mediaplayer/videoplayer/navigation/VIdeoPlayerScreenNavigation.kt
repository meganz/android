package mega.privacy.android.app.mediaplayer.videoplayer.navigation

import androidx.compose.material.ScaffoldState
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import kotlinx.serialization.Serializable
import mega.privacy.android.app.mediaplayer.videoplayer.view.VideoPlayerScreen
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModel

@Serializable
internal object VideoPlayerScreen

internal const val VIDEO_PLAYER_SCREEN_ROUTE = "VideoPlayerScreen"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.videoPlayerScreen(
    bottomSheetNavigator: BottomSheetNavigator,
    scaffoldState: ScaffoldState,
    viewModel: VideoPlayerViewModel,
    player: ExoPlayer?,
    playQueueButtonClicked: () -> Unit,
) {
    composable<VideoPlayerScreen> {
        VideoPlayerScreen(
            bottomSheetNavigator = bottomSheetNavigator,
            scaffoldState = scaffoldState,
            viewModel = viewModel,
            player = player,
            playQueueButtonClicked = playQueueButtonClicked
        )
    }
}