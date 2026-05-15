package mega.privacy.android.app.presentation.videoplayer.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.serialization.Serializable
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.mediaplayer.SelectSubtitleFileViewModel
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModelV2
import mega.privacy.android.app.presentation.videoplayer.model.SubtitleSelectedStatus
import mega.privacy.android.app.presentation.videoplayer.view.VideoPlayerSelectSubtitleView
import mega.privacy.mobile.analytics.event.AddSubtitlePressedEvent
import mega.privacy.mobile.analytics.event.CancelSelectSubtitlePressedEvent

/**
 * Select subtitle NavKey for the revamped video player.
 */
@Serializable
internal data object VideoPlayerSelectSubtitleScreenNavKey : NavKey

internal fun EntryProviderScope<NavKey>.videoPlayerSelectSubtitleScreen(
    viewModel: VideoPlayerViewModelV2,
    onBack: () -> Unit,
) {
    entry<VideoPlayerSelectSubtitleScreenNavKey> {
        val systemUiController = rememberSystemUiController()
        val selectSubtitleViewModel = hiltViewModel<SelectSubtitleFileViewModel>()

        LaunchedEffect(Unit) {
            systemUiController.isSystemBarsVisible = true
        }

        VideoPlayerSelectSubtitleView(
            onAddSubtitle = { info ->
                Analytics.tracker.trackEvent(AddSubtitlePressedEvent)
                viewModel.updateSubtitleSelectedStatus(
                    SubtitleSelectedStatus.AddSubtitleItem,
                    info
                )
                selectSubtitleViewModel.clearSelectedItem()
                onBack()
            },
            onBackPressed = {
                Analytics.tracker.trackEvent(CancelSelectSubtitlePressedEvent)
                onBack()
                viewModel.updateShowSubtitleDialog(false)
            }
        )
    }
}
