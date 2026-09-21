package mega.privacy.android.feature.mediaplayer.presentation.miniplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.feature.mediaplayer.data.gateway.AudioMediaControllerGateway
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel behind the Compose mini player.
 *
 * Not wired to any host yet: the mini player is still rendered by the Media3 `PlayerView` in
 * `mini_audio_player.xml`. Nothing constructs this ViewModel, so no [AudioMediaControllerGateway]
 * is created and the shipped player is unaffected. Note that the gateway connects to the session
 * in its own `init`, so instantiating this ViewModel does open a `MediaController`.
 */
@HiltViewModel
internal class AudioMiniPlayerViewModel @Inject constructor(
    private val gateway: AudioMediaControllerGateway,
) : ViewModel() {

    val uiState: StateFlow<AudioMiniPlayerUiState> by lazy(LazyThreadSafetyMode.NONE) {
        gateway.playerState
            .map { state ->
                if (state.mediaItemCount == 0 || state.isSessionEnded) {
                    AudioMiniPlayerUiState.Hidden
                } else {
                    AudioMiniPlayerUiState.Data(
                        title = state.title.orEmpty(),
                        artist = state.artist,
                        isPlaying = state.isPlaying,
                        isLoading = state.isBuffering,
                        progress = progressOf(state.currentPositionMs, state.durationMs),
                    )
                }
            }
            .catch {
                // catch completes the upstream flow, so without a fallback the bar would stay
                // frozen on its last state: visible, with a stale title, while nothing plays.
                Timber.e(it, "Failed to collect mini player state")
                emit(AudioMiniPlayerUiState.Hidden)
            }
            .asUiStateFlow(viewModelScope, AudioMiniPlayerUiState.Hidden)
    }

    /** Toggles playback, mirroring the play/pause button of the full-screen player. */
    fun onPlayPauseClicked() {
        val state = uiState.value
        if (state is AudioMiniPlayerUiState.Data && state.isPlaying) gateway.pause() else gateway.play()
    }

    override fun onCleared() {
        runCatching { gateway.release() }
            .onFailure { Timber.e(it, "Failed to release the mini player media controller") }
        super.onCleared()
    }

    private fun progressOf(positionMs: Long, durationMs: Long): Float =
        if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}
