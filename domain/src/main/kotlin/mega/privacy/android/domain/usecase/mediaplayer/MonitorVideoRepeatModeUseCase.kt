package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * Use case for monitoring video repeat mode
 */
class MonitorVideoRepeatModeUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Invoke
     *
     * @return Flow of Int
     */
    operator fun invoke() = mediaPlayerRepository.monitorVideoRepeatMode()
}