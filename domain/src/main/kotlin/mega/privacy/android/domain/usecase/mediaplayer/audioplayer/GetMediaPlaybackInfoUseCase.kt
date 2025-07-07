package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

class GetMediaPlaybackInfoUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Monitor the playback times
     *
     * @return Flow<List<MediaPlaybackInfo>>
     */
    suspend operator fun invoke(handle: Long): MediaPlaybackInfo? =
        mediaPlayerRepository.getMediaPlaybackInfo(handle)
}