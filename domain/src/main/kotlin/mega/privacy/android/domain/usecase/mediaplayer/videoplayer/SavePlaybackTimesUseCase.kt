package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for saving playback times
 */
class SavePlaybackTimesUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository
) {

    /**
     * Save the playback times
     */
    suspend operator fun invoke() = mediaPlayerRepository.savePlaybackTimes()
}