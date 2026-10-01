package mega.privacy.android.feature.mediaplayer.videoplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerUiState
import javax.inject.Inject

/**
 * ViewModel of the revamped video player.
 *
 * Skeleton: it holds only the state contract so the route has something to render. Playback,
 * the queue, subtitles, gestures and the rest are added one feature at a time, each against its
 * entry in `.ref/video-player-feature-inventory.md`, with the frozen legacy player
 * (`VideoPlayerViewModelV2` in `:app`) as the specification.
 *
 * The player itself is reached through
 * [mega.privacy.android.feature.mediaplayer.data.gateway.VideoMediaControllerGateway], which is
 * already in place; it is injected by the first ticket that actually plays something.
 */
@HiltViewModel
internal class VideoPlayerViewModel @Inject constructor() : ViewModel() {

    val uiState: StateFlow<VideoPlayerUiState> by lazy(LazyThreadSafetyMode.NONE) {
        flowOf<VideoPlayerUiState>(VideoPlayerUiState.Data)
            .asUiStateFlow(viewModelScope, VideoPlayerUiState.Loading)
    }
}
