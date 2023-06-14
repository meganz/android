package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * Use case for setting audio repeat mode
 */
class SetAudioRepeatModeUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Invoke
     *
     * @param value Int value of audio repeat mode
     */
    suspend operator fun invoke(value: Int) = mediaPlayerRepository.setAudioRepeatMode(value)
}