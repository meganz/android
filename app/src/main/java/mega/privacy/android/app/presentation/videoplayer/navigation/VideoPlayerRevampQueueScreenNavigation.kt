package mega.privacy.android.app.presentation.videoplayer.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerRevampViewModel
import mega.privacy.android.app.presentation.videoplayer.view.VideoPlayerRevampQueueScreen

/**
 * Play queue route for the revamped video player only. Legacy compose graph uses
 * [mega.privacy.android.app.mediaplayer.videoplayer.navigation.VideoQueueScreen].
 */
@Serializable
internal object VideoPlayerRevampQueueScreen

internal fun NavGraphBuilder.videoPlayerRevampQueueScreen(
    navHostController: NavHostController,
    viewModel: VideoPlayerRevampViewModel,
) {
    composable<VideoPlayerRevampQueueScreen> {
        VideoPlayerRevampQueueScreen(
            navHostController = navHostController,
            viewModel = viewModel,
        )
    }
}
