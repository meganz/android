package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject


/**
 * Use case for broadcasting when audio output has changed.
 */
class BroadcastAudioOutputUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param audioDevice    [AudioDevice]
     */
    suspend operator fun invoke(audioDevice: AudioDevice) =
        callRepository.broadcastAudioOutput(audioDevice)
}