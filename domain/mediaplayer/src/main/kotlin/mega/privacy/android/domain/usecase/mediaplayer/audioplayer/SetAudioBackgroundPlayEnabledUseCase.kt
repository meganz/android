package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * Use case for setting the value of AudioBackgroundPlayEnabled
 */
class SetAudioBackgroundPlayEnabledUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Invoke
     *
     * @param value true is enable audio background play, otherwise is false.
     */
    suspend operator fun invoke(value: Boolean) =
        mediaPlayerRepository.setAudioBackgroundPlayEnabled(value)
}