package mega.privacy.android.app.mediaplayer.videoplayer.navigation

import androidx.compose.material.ScaffoldState
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

internal const val VideoPlayerNavigationRoute = "videoPlayer"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.videoPlayerComposeNavigationGraph(
    bottomSheetNavigator: BottomSheetNavigator,
    scaffoldState: ScaffoldState,
    player: ExoPlayer?,
) {
    navigation(
        startDestination = VideoPlayerScreenRoute,
        route = VideoPlayerNavigationRoute,
    ) {
        videoPlayerScreen(
            bottomSheetNavigator = bottomSheetNavigator,
            scaffoldState = scaffoldState,
            player = player,
        )
    }
}

internal fun NavHostController.navigateToVideoPlayerComposeGraph(navOptions: NavOptions) =
    navigate(VideoPlayerScreenRoute, navOptions)