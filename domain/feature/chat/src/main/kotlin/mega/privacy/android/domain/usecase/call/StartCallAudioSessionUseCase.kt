package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.repository.CallAudioRouteRepository
import javax.inject.Inject

/**
 * Starts routing call audio.
 */
class StartCallAudioSessionUseCase @Inject constructor(
    private val callAudioRouteRepository: CallAudioRouteRepository,
) {

    /**
     * Starts a call audio session that routes to [preferredDevice] as soon as it is available.
     * Calling it while a session is active only replaces the preferred device.
     *
     * @throws IllegalArgumentException if [preferredDevice] is [AudioDevice.None].
     */
    suspend operator fun invoke(preferredDevice: AudioDevice) {
        require(preferredDevice != AudioDevice.None) { "The preferred device must be a real device" }
        callAudioRouteRepository.startSession(preferredDevice)
    }
}
