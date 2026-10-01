package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * Use case for setting the value of AudioShuffleEnabled
 */
class SetAudioShuffleEnabledUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Invoke
     *
     * @param value true is shuffled, otherwise is false.
     */
    suspend operator fun invoke(value: Boolean) =
        mediaPlayerRepository.setAudioShuffleEnabled(value)
}