package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * Use case for monitoring the value of AudioContinuousPlaybackEnabled.
 *
 * Defaults to enabled: playing the whole queue is the behaviour users get without ever
 * opening the setting.
 */
class MonitorAudioContinuousPlaybackEnabledUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Invoke
     *
     * @return Flow of Boolean
     */
    operator fun invoke() = mediaPlayerRepository.monitorAudioContinuousPlaybackEnabled()
        .map { it ?: true }
}
