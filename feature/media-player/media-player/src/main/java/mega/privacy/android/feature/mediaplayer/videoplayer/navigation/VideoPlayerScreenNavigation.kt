package mega.privacy.android.feature.mediaplayer.videoplayer.navigation

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.VideoPlayerScreen
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.VideoPlayerViewModel

/**
 * Navigation key for the revamped video player screen.
 *
 * Carries only a [launchId] so the back stack stays small; the launch payload is looked up from a
 * holder. The holder still lives in `:app` together with the launch-source model it stores, which
 * the legacy player shares, so this skeleton does not read it yet — that happens with the launch
 * and queue-loading feature.
 *
 * The play queue and subtitle selection are not separate destinations: they are rendered as
 * in-place overlays, so they share this entry's [VideoPlayerViewModel] and therefore one player.
 */
@Serializable
@Parcelize
data class VideoPlayerScreenNavKey(val launchId: String) : NavKey, Parcelable

internal fun EntryProviderScope<NavKey>.videoPlayerScreen() {
    entry<VideoPlayerScreenNavKey> {
        val viewModel = hiltViewModel<VideoPlayerViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        VideoPlayerScreen(uiState = uiState)
    }
}
