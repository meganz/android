package mega.privacy.android.domain.entity.mediaplayer

/**
 * Data class representing media playback information.
 *
 * @property mediaHandle The unique identifier for the media item.
 * @property totalDuration The total duration of the media in milliseconds.
 * @property currentPosition The current playback position in milliseconds.
 * @property mediaType The type of media (e.g., audio, video).
 */
data class MediaPlaybackInfo(
    val mediaHandle: Long,
    val totalDuration: Long,
    val currentPosition: Long,
    val mediaType: MediaType,
)