package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for monitoring when the audio output is changed.
 */
class MonitorAudioOutputUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    /**
     * Invoke
     *
     * @return Flow of AudioDevice
     */
    operator fun invoke() = callRepository.monitorAudioOutput()
}