package mega.privacy.android.app.mediaplayer.videoplayer.navigation

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.compose.material.navigation.BottomSheetNavigator
import kotlinx.serialization.Serializable
import mega.privacy.android.app.mediaplayer.videoplayer.view.LegacyVideoPlayerScreen
import mega.privacy.android.app.presentation.videoplayer.LegacyVideoPlayerViewModel

@Serializable
internal object LegacyVideoPlayerScreen

internal fun NavGraphBuilder.legacyVideoPlayerScreen(
    navHostController: NavHostController,
    bottomSheetNavigator: BottomSheetNavigator,
    scaffoldState: ScaffoldState,
    viewModel: LegacyVideoPlayerViewModel,
    player: ExoPlayer?,
    handleAutoReplayIfPaused: () -> Unit,
    playQueueButtonClicked: () -> Unit,
) {
    composable<LegacyVideoPlayerScreen> {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) {
            handleAutoReplayIfPaused()
        }

        LaunchedEffect(uiState.navigateToSelectSubtitleScreen) {
            if (uiState.navigateToSelectSubtitleScreen) {
                navHostController.navigate(LegacySelectSubtitleScreen)
                viewModel.updateNavigateToSelectSubtitle(false)
            }
        }

        LegacyVideoPlayerScreen(
            bottomSheetNavigator = bottomSheetNavigator,
            scaffoldState = scaffoldState,
            viewModel = viewModel,
            player = player,
            playQueueButtonClicked = playQueueButtonClicked
        )
    }
}