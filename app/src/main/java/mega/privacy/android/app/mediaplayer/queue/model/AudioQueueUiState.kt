package mega.privacy.android.app.mediaplayer.queue.model

/**
 * Audio queue UI state
 *
 * @property items list of media queue item UI entities
 * @property isPaused whether the playing audio is paused
 * @property currentPlayingPosition the current playing position of the audio
 */
data class AudioQueueUiState(
    val items: List<MediaQueueItemUiEntity>,
    val isPaused: Boolean = false,
    val currentPlayingPosition: String = "00:00",
)