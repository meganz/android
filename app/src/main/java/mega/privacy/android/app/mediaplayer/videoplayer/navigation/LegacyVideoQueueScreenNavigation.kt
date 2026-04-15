package mega.privacy.android.app.mediaplayer.videoplayer.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.videoplayer.LegacyVideoPlayerViewModel
import mega.privacy.android.app.presentation.videoplayer.view.LegacyVideoQueueScreen

@Serializable
internal object LegacyVideoQueueScreen

internal fun NavGraphBuilder.legacyVideoQueueScreen(
    navHostController: NavHostController,
    legacyVideoPlayerViewModel: LegacyVideoPlayerViewModel,
) {
    composable<LegacyVideoQueueScreen> {
        LegacyVideoQueueScreen(
            navHostController = navHostController,
            viewModel = legacyVideoPlayerViewModel
        )
    }
}