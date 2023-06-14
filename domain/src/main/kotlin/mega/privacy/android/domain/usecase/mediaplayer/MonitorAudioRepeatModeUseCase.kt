package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * Use case for monitoring audio repeat mode
 */
class MonitorAudioRepeatModeUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository
) {
    /**
     * Invoke
     *
     * @return Flow of Boolean
     */
    operator fun invoke() = mediaPlayerRepository.monitorAudioRepeatMode()
}