package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.repository.CallAudioRouteRepository
import javax.inject.Inject

/**
 * Routes call audio to a device.
 */
class SelectCallAudioDeviceUseCase @Inject constructor(
    private val callAudioRouteRepository: CallAudioRouteRepository,
) {

    /**
     * Routes call audio to [device]. Does nothing when no call audio session is active or when
     * [device] is not available.
     */
    suspend operator fun invoke(device: AudioDevice) = callAudioRouteRepository.select(device)
}
