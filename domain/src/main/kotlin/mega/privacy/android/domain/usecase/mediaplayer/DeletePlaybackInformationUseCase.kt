package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for deleting playback information
 */
class DeletePlaybackInformationUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Delete playback information
     *
     * @param mediaId the media id of deleted item
     */
    suspend operator fun invoke(mediaId: Long) =
        mediaPlayerRepository.deletePlaybackInformation(mediaId)
}