package mega.privacy.android.app.mediaplayer.videoplayer.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModel
import mega.privacy.android.app.presentation.videoplayer.view.VideoQueueScreen

@Serializable
internal object VideoQueueScreen

internal fun NavGraphBuilder.videoQueueScreen(
    navHostController: NavHostController,
    videoPlayerViewModel: VideoPlayerViewModel,
) {
    composable<VideoQueueScreen> {
        VideoQueueScreen(
            navHostController = navHostController,
            viewModel = videoPlayerViewModel
        )
    }
}