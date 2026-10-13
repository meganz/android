package mega.privacy.android.feature.mediaplayer.presentation.miniplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.feature.mediaplayer.data.gateway.AudioMediaControllerGateway
import mega.privacy.android.feature.mediaplayer.data.AudioPlayerSessionSignals
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel behind the Compose mini player.
 *
 * The gateway connects to the session in its own `init`, so constructing this ViewModel opens a
 * `MediaController`. A host only does so while both audio player flags are on.
 */
@HiltViewModel
internal class AudioMiniPlayerViewModel @Inject constructor(
    private val gateway: AudioMediaControllerGateway,
    sessionSignals: AudioPlayerSessionSignals,
) : ViewModel() {

    /**
     * Tracked apart from [uiState] so that a session ending while nothing collects the bar — the
     * host activity in the background — is still recorded, and the next session reconnects.
     */
    private val isSessionEnded: StateFlow<Boolean> =
        gateway.playerState
            .map { it.isSessionEnded }
            .catch {
                // catch completes the flow, so without a fallback the gate would stay false
                // for good — and a controller that errored is exactly when the next session
                // needs to reconnect.
                Timber.e(it, "Failed to track the mini player session state")
                emit(true)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        // This ViewModel lives as long as its host activity, so it outlives the sessions it
        // shows. A MediaController dies with its session, and nothing reconnects it: the
        // full-screen player reconnects the controller it owns, not this one. Without this the
        // bar never comes back after the first session ends.
        viewModelScope.launch {
            sessionSignals.sessionStarts
                .filter { isSessionEnded.value }
                .catch { Timber.e(it, "Failed to collect mini player session starts") }
                .collect {
                    runCatching { gateway.reconnect() }
                        .onFailure { Timber.e(it, "Failed to reconnect the mini player") }
                }
        }
    }

    val uiState: StateFlow<AudioMiniPlayerUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            gateway.playerState,
            sessionSignals.isDismissed,
        ) { state, isNotificationDismissed ->
            if (state.mediaItemCount == 0 || state.isSessionEnded) {
                AudioMiniPlayerUiState.Hidden
            } else {
                AudioMiniPlayerUiState.Data(
                    title = state.title.orEmpty(),
                    artist = state.artist,
                    isPlaying = state.isPlaying,
                    // A dismissed notification parks playback, so the spinner would never
                    // resolve; the play button is shown instead.
                    isLoading = state.isBuffering && !isNotificationDismissed,
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

    /** Moves to the next track, for a swipe across the bar. */
    fun onSwipedToNext() {
        gateway.skipToNext()
    }

    /**
     * Moves to the previous track, for a swipe across the bar.
     *
     * A swipe goes straight there. The restart-first rule the previous button follows would make
     * the same gesture mean two different things depending on how far the track had played.
     */
    fun onSwipedToPrevious() {
        gateway.skipToPreviousMediaItem()
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
