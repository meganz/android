package mega.privacy.android.app.mediaplayer.model

/**
 * Data class representing the UI state of the audio player
 *
 * @property isSpeedPopupShown Boolean indicating if the speed selection popup is shown
 * @property currentSpeedPlayback The currently selected speed playback item
 */
data class AudioPlayerUiState(
    val isSpeedPopupShown: Boolean = false,
    val currentSpeedPlayback: SpeedPlaybackItem = AudioSpeedPlaybackItem.PlaybackSpeed_1X,
)
