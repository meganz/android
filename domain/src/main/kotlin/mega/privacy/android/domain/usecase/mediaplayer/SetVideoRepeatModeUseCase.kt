package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * Use case for setting video repeat mode
 */
class SetVideoRepeatModeUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Invoke
     *
     * @param value Int value of video repeat mode
     */
    suspend operator fun invoke(value: Int) = mediaPlayerRepository.setVideoRepeatMode(value)
}