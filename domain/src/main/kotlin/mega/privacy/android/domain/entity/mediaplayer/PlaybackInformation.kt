package mega.privacy.android.domain.entity.mediaplayer

/**
 * The entity for playback information
 *
 * @property mediaId the media id of media item
 * @property totalDuration the total duration of media item
 * @property currentPosition the current position of media item
 */
data class PlaybackInformation(
    val mediaId: Long?,
    val totalDuration: Long,
    val currentPosition: Long,
)
