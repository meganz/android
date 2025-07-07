package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

class SaveAudioPlaybackInfoUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Save the audio playback info
     */
    suspend operator fun invoke() = mediaPlayerRepository.saveAudioPlaybackInfo()
}