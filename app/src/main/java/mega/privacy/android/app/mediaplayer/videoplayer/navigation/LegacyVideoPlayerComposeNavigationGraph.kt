package mega.privacy.android.app.mediaplayer.videoplayer.navigation

import androidx.compose.material.ScaffoldState
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import androidx.compose.material.navigation.BottomSheetNavigator
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.videoplayer.LegacyVideoPlayerViewModel

@Serializable
internal object LegacyVideoPlayerNavigationGraph

internal fun NavGraphBuilder.legacyVideoPlayerComposeNavigationGraph(
    navHostController: NavHostController,
    bottomSheetNavigator: BottomSheetNavigator,
    scaffoldState: ScaffoldState,
    viewModel: LegacyVideoPlayerViewModel,
    player: ExoPlayer?,
    handleAutoReplayIfPaused: () -> Unit,
) {
    navigation<LegacyVideoPlayerNavigationGraph>(
        startDestination = LegacyVideoPlayerScreen,
    ) {
        legacyVideoPlayerScreen(
            navHostController = navHostController,
            bottomSheetNavigator = bottomSheetNavigator,
            scaffoldState = scaffoldState,
            viewModel = viewModel,
            player = player,
            handleAutoReplayIfPaused = handleAutoReplayIfPaused,
        ) {
            navHostController.navigate(LegacyVideoQueueScreen)
        }

        legacyVideoQueueScreen(
            navHostController = navHostController,
            legacyVideoPlayerViewModel = viewModel
        )

        legacySelectSubtitleScreen(
            navHostController = navHostController,
            legacyVideoPlayerViewModel = viewModel
        )
    }
}