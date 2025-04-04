package mega.privacy.android.app.mediaplayer.videoplayer.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModel
import mega.privacy.android.app.presentation.videoplayer.view.VideoQueueScreen

@Serializable
internal object VideoQueueScreen

internal const val VIDEO_QUEUE_SCREEN_ROUTE = "VideoQueueScreen"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.videoQueueScreen(
    videoPlayerViewModel: VideoPlayerViewModel,
) {
    composable<VideoQueueScreen> {
        VideoQueueScreen(
            viewModel = videoPlayerViewModel
        )
    }
}