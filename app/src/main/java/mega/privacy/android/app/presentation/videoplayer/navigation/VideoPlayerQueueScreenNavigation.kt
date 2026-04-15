package mega.privacy.android.app.presentation.videoplayer.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModelV2
import mega.privacy.android.app.presentation.videoplayer.view.VideoPlayerQueueScreen

/**
 * Play queue NavKey for the video player.
 */
@Serializable
internal data object VideoPlayerQueueScreenNavKey : NavKey

internal fun EntryProviderScope<NavKey>.videoPlayerQueueScreen(
    viewModel: VideoPlayerViewModelV2,
    onBack: () -> Unit,
) {
    entry<VideoPlayerQueueScreenNavKey> {
        VideoPlayerQueueScreen(
            viewModel = viewModel,
            onBack = onBack,
        )
    }
}
