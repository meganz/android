package mega.privacy.android.app.mediaplayer.model

import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode

/**
 * The state for updating the video player UI
 *
 * @property subtitleDisplayState the state regarding subtitle
 * @property isFullScreen current video shown state, true is full screen, otherwise is false
 * @property isSpeedPopupShown speed playback popup whether is shown, true is shown, otherwise is false
 * @property currentSpeedPlayback current SpeedPlaybackItem
 * @property isVideoOptionPopupShown video option popup whether is shown, true is shown, otherwise is false
 * @property videoRepeatToggleMode videoRepeatToggleMode
 */
data class VideoPlayerUiState(
    val subtitleDisplayState: SubtitleDisplayState = SubtitleDisplayState(),
    val isFullScreen: Boolean = false,
    val isSpeedPopupShown: Boolean = false,
    val currentSpeedPlayback: SpeedPlaybackItem = SpeedPlaybackItem.PLAYBACK_SPEED_1_X,
    val isVideoOptionPopupShown: Boolean = false,
    val videoRepeatToggleMode: RepeatToggleMode = RepeatToggleMode.REPEAT_NONE
)