package mega.privacy.android.app.mediaplayer.model

/**
 * The state for updating the video player UI
 *
 * @property subtitleDisplayState the state regarding subtitle
 * @property isFullScreen current video shown state, true is full screen, otherwise is false
 */
data class VideoPlayerUiState(
    val subtitleDisplayState: SubtitleDisplayState = SubtitleDisplayState(),
    val isFullScreen: Boolean = false
)