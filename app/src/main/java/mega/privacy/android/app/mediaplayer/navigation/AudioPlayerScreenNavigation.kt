package mega.privacy.android.app.mediaplayer.navigation

import android.os.Parcelable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.app.mediaplayer.AudioPlayerLaunchSourceHolder
import mega.privacy.android.app.mediaplayer.AudioPlayerScreen
import mega.privacy.android.app.mediaplayer.AudioPlayerViewModel
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Navigation key for the revamped audio player Compose screen.
 *
 * Only carries a [launchId] to look up the full launch payload from
 * [AudioPlayerLaunchSourceHolder], avoiding [android.os.TransactionTooLargeException].
 */
@Serializable
@Parcelize
data class AudioPlayerScreenNavKey(val launchId: String) : NavKey, Parcelable

// TODO: Use navigationHandler for back navigation from audio player
@Suppress("UNUSED_PARAMETER")
internal fun EntryProviderScope<NavKey>.audioPlayerScreen(
    navigationHandler: NavigationHandler,
    launchSourceHolder: AudioPlayerLaunchSourceHolder,
) {
    entry<AudioPlayerScreenNavKey> { navKey ->
        val viewModel = hiltViewModel<AudioPlayerViewModel>()

        LaunchedEffect(navKey.launchId) {
            val intent = launchSourceHolder.consume(navKey.launchId) ?: return@LaunchedEffect
            viewModel.startPlayback(intent)
        }

        // onDispose fires only when this entry is removed from the Navigation3 back stack
        // (i.e. the user navigates away). Activity re-creation on configuration change does NOT
        // remove the entry, so stopPlayer() is NOT called during rotation or other config changes.
        DisposableEffect(navKey.launchId) {
            onDispose {
                viewModel.stopPlayer()
            }
        }

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        AudioPlayerScreen(
            uiState = uiState,
            onPlayPauseClicked = viewModel::togglePlayPause,
            onSeekTo = viewModel::seekTo,
            onNextClicked = viewModel::skipToNext,
            onPreviousClicked = viewModel::skipToPrevious,
            onShuffleClicked = viewModel::toggleShuffle,
            onRepeatClicked = viewModel::cycleRepeatMode,
            onPlaylistClicked = { /* TODO: navigate to queue */ },
            onScreenClicked = { /* TODO: toggle toolbar */ },
        )
    }
}
