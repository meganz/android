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
import mega.privacy.android.app.presentation.videoplayer.LegacyVideoPlayerViewModel
import mega.privacy.android.app.presentation.videoplayer.model.SubtitleSelectedStatus
import mega.privacy.mobile.analytics.event.AddSubtitlePressedEvent
import mega.privacy.mobile.analytics.event.CancelSelectSubtitlePressedEvent

@Serializable
internal object LegacySelectSubtitleScreen

internal fun NavGraphBuilder.legacySelectSubtitleScreen(
    navHostController: NavHostController,
    legacyVideoPlayerViewModel: LegacyVideoPlayerViewModel,
) {
    composable<LegacySelectSubtitleScreen> { backStackEntry ->
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
                legacyVideoPlayerViewModel.updateSubtitleSelectedStatus(
                    SubtitleSelectedStatus.AddSubtitleItem,
                    info
                )
                selectSubtitleViewModel.clearSelectedItem()
                navHostController.popBackStack()
            },
            onBackPressed = {
                Analytics.tracker.trackEvent(CancelSelectSubtitlePressedEvent)
                navHostController.popBackStack()
                legacyVideoPlayerViewModel.updateShowSubtitleDialog(false)
            }
        )
    }
}