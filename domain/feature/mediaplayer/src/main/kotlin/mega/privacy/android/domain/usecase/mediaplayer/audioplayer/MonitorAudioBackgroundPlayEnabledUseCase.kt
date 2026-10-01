package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * Use case for monitoring the value of AudioBackgroundPlayEnabled
 */
class MonitorAudioBackgroundPlayEnabledUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Invoke
     *
     * @return Flow of Boolean
     */
    operator fun invoke() = mediaPlayerRepository.monitorAudioBackgroundPlayEnabled()
        .map { it ?: true }
}