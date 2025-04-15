package mega.privacy.android.app.mediaplayer.videoplayer.navigation

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
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModel
import mega.privacy.android.app.presentation.videoplayer.model.SubtitleSelectedStatus
import mega.privacy.mobile.analytics.event.AddSubtitlePressedEvent
import mega.privacy.mobile.analytics.event.CancelSelectSubtitlePressedEvent

@Serializable
internal object SelectSubtitleScreen

internal fun NavGraphBuilder.selectSubtitleScreen(
    navHostController: NavHostController,
    videoPlayerViewModel: VideoPlayerViewModel,
) {
    composable<SelectSubtitleScreen> { backStackEntry ->
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
                videoPlayerViewModel.updateSubtitleSelectedStatus(
                    SubtitleSelectedStatus.AddSubtitleItem,
                    info
                )
                info?.let {
                    selectSubtitleViewModel.itemClickedUpdate(it)
                }
                navHostController.popBackStack()
            },
            onBackPressed = {
                Analytics.tracker.trackEvent(CancelSelectSubtitlePressedEvent)
                navHostController.popBackStack()
                videoPlayerViewModel.updateShowSubtitleDialog(false)
            }
        )
    }
}