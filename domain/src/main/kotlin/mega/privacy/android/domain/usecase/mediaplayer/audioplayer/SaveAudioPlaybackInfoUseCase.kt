package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

@Deprecated(
    message = "Only used by the legacy audio player; remove together with it. " +
        "The revamped player persists every update via TrackAndSaveAudioPlaybackInfoUseCase, " +
        "so no separate save step is needed.",
)
class SaveAudioPlaybackInfoUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Save the audio playback info
     */
    suspend operator fun invoke() = mediaPlayerRepository.saveAudioPlaybackInfo()
}