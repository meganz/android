package mega.privacy.android.domain.entity.mediaplayer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The entity for playback information
 *
 * @property mediaId the media id of media item
 * @property totalDuration the total duration of media item
 * @property currentPosition the current position of media item
 */
@Serializable
data class PlaybackInformation(
    @SerialName("mediaId") val mediaId: Long?,
    @SerialName("totalDuration") val totalDuration: Long,
    @SerialName("currentPosition") val currentPosition: Long,
)
