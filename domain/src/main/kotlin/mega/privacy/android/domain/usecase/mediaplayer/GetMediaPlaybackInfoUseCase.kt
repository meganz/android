package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * Reads the stored playback position of a media item, whatever its type.
 *
 * Callers that only accept one media type must check [MediaPlaybackInfo.mediaType] themselves:
 * the store is shared between the audio and video players.
 */
class GetMediaPlaybackInfoUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Invoke
     *
     * @param handle the media handle to look up
     * @return the stored [MediaPlaybackInfo], or null when nothing is stored for [handle]
     */
    suspend operator fun invoke(handle: Long): MediaPlaybackInfo? =
        mediaPlayerRepository.getMediaPlaybackInfo(handle)
}