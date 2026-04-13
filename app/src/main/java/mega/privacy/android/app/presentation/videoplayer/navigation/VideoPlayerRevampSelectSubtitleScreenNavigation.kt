package mega.privacy.android.app.presentation.videoplayer.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.serialization.Serializable
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.mediaplayer.SelectSubtitleComposeView
import mega.privacy.android.app.mediaplayer.SelectSubtitleFileViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose.sharedViewModel
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerRevampViewModel
import mega.privacy.android.app.presentation.videoplayer.model.SubtitleSelectedStatus
import mega.privacy.mobile.analytics.event.AddSubtitlePressedEvent
import mega.privacy.mobile.analytics.event.CancelSelectSubtitlePressedEvent

/**
 * Select subtitle route for the revamped video player only. Legacy graph uses
 * [mega.privacy.android.app.mediaplayer.videoplayer.navigation.SelectSubtitleScreen].
 */
@Serializable
internal object VideoPlayerRevampSelectSubtitleScreen

internal fun NavGraphBuilder.videoPlayerRevampSelectSubtitleScreen(
    navHostController: NavHostController,
    viewModel: VideoPlayerRevampViewModel,
) {
    composable<VideoPlayerRevampSelectSubtitleScreen> { backStackEntry ->
        val selectSubtitleViewModel =
            backStackEntry.sharedViewModel<SelectSubtitleFileViewModel>(navHostController)
        val systemUiController = rememberSystemUiController()

        LaunchedEffect(Unit) {
            systemUiController.isSystemBarsVisible = true
        }

        SelectSubtitleComposeView(
            viewModel = selectSubtitleViewModel,
            onAddSubtitle = { info ->
                Analytics.tracker.trackEvent(AddSubtitlePressedEvent)
                viewModel.updateSubtitleSelectedStatus(
                    SubtitleSelectedStatus.AddSubtitleItem,
                    info
                )
                selectSubtitleViewModel.clearSelectedItem()
                navHostController.popBackStack()
            },
            onBackPressed = {
                Analytics.tracker.trackEvent(CancelSelectSubtitlePressedEvent)
                navHostController.popBackStack()
                viewModel.updateShowSubtitleDialog(false)
            }
        )
    }
}
