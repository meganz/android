package mega.privacy.android.feature.mediaplayer.presentation.miniplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful entry point of the Compose mini player.
 *
 * Renders nothing while no playback session exists, so a host can place it unconditionally.
 *
 * Opening the full-screen player and closing the session are both left to the host: the
 * destination belongs to the app module, and ending a session means stopping
 * `AudioPlayerService`, which only the app module can reach.
 *
 * Compose this under an activity-scoped [androidx.lifecycle.ViewModelStoreOwner]. The ViewModel
 * is resolved with `hiltViewModel()`, so it follows the nearest owner, and each instance opens its
 * own `MediaController`. Placed inside a navigation entry it would be torn down and reconnected on
 * every destination change, where the `PlayerView` bar it replaces keeps one connection for the
 * whole activity.
 *
 * Not used by any host yet; see `AudioMiniPlayer` for why.
 *
 * @param onPlayerClicked Called when the bar is tapped.
 * @param onCloseClicked Called when the close button is tapped; the host is expected to end the
 * playback session, not merely pause it, or the bar stays on screen.
 * @param modifier Modifier.
 * @param onVisibilityChanged Called whenever the bar appears or disappears, so a host can adjust
 * the layout around it.
 */
@Composable
fun AudioMiniPlayerRoute(
    onPlayerClicked: () -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onVisibilityChanged: ((Boolean) -> Unit)? = null,
) {
    val viewModel = hiltViewModel<AudioMiniPlayerViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isVisible = state is AudioMiniPlayerUiState.Data

    LaunchedEffect(isVisible) { onVisibilityChanged?.invoke(isVisible) }

    (state as? AudioMiniPlayerUiState.Data)?.let { data ->
        AudioMiniPlayer(
            state = data,
            onPlayPauseClicked = viewModel::onPlayPauseClicked,
            onCloseClicked = onCloseClicked,
            onPlayerClicked = onPlayerClicked,
            modifier = modifier,
        )
    }
}
