package mega.privacy.android.app.mediaplayer.model

/**
 * Data class representing the UI state of the audio player
 *
 * @property isSpeedPopupShown Boolean indicating if the speed selection popup is shown
 * @property currentSpeedPlayback The currently selected speed playback item
 * @property currentPlayingHandle The handle of the currently playing audio item
 * @property currentPlayingItemName The name of the currently playing audio item
 * @property playbackPosition The current playback position in milliseconds, or null if not applicable
 * @property showPlaybackDialog Boolean indicating if the playback position dialog should be shown
 */
data class AudioPlayerUiState(
    val isSpeedPopupShown: Boolean = false,
    val currentSpeedPlayback: SpeedPlaybackItem = AudioSpeedPlaybackItem.PlaybackSpeed_1X,
    val currentPlayingHandle: Long = -1,
    val currentPlayingItemName: String? = null,
    val playbackPosition: Long? = null,
    val showPlaybackDialog: Boolean = false
)
