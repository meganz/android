package mega.privacy.android.app.presentation.videoplayer.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModelV2
import mega.privacy.android.app.presentation.videoplayer.view.VideoPlayerQueueScreen
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

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
        // OriginalTheme (not AndroidTheme): the screen is built from original-core-ui
        // components, which resolve their colors from the legacy MaterialTheme palette
        // that only OriginalTheme provides. Forced dark to match the player screen.
        OriginalTheme(isDark = true) {
            VideoPlayerQueueScreen(
                viewModel = viewModel,
                onBack = onBack,
            )
        }
    }
}
