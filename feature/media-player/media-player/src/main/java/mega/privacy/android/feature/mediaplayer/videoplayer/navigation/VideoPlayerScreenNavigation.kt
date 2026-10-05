package mega.privacy.android.feature.mediaplayer.videoplayer.navigation

import android.os.Parcelable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.VideoPlayerScreen
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.VideoPlayerViewModel
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerUiState
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Navigation key for the revamped video player screen.
 *
 * Carries only a [launchId] so the back stack stays small; the launch payload itself is held by
 * [VideoPlayerLaunchSourceHolder], which hands it out once.
 *
 * The play queue and subtitle selection are not separate destinations: they are rendered as
 * in-place overlays, so they share this entry's [VideoPlayerViewModel] and therefore one player.
 */
@Serializable
@Parcelize
data class VideoPlayerScreenNavKey(val launchId: String) : NavKey, Parcelable

internal fun EntryProviderScope<NavKey>.videoPlayerScreen(
    navigationHandler: NavigationHandler,
) {
    entry<VideoPlayerScreenNavKey> { navKey ->
        val viewModel = hiltViewModel<VideoPlayerViewModel, VideoPlayerViewModel.Factory>(
            creationCallback = { factory -> factory.create(navKey.launchId) }
        )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        // Leaving is the whole response to an unusable launch, and it belongs here rather than in
        // the screen: the screen never takes a navigation handler. Keyed on the decision rather
        // than on the state, so that adding a field that changes while playing does not restart
        // this effect on every emission.
        val launchWasUnusable = uiState is VideoPlayerUiState.InvalidLaunchSource
        LaunchedEffect(launchWasUnusable) {
            if (launchWasUnusable) navigationHandler.back()
        }

        VideoPlayerScreen(
            uiState = uiState,
            onSurfaceReady = viewModel::onSurfaceReady,
            onSurfaceReleased = viewModel::onSurfaceReleased,
        )
    }
}
