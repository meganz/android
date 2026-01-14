package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for deleting audio playback info
 */
class DeleteAudioPlaybackInfoUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Delete audio playback info
     *
     * @param mediaHandle the media handle of deleted item
     */
    suspend operator fun invoke(mediaHandle: Long) =
        mediaPlayerRepository.deleteMediaPlaybackInfo(mediaHandle)
}