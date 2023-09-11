package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use Case to Start/Stop video device
 *
 * @property callRepository     [CallRepository]
 */
class StartVideoDeviceUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Start Video Device
     *
     * @param start  True to start the video device, false to stop.
     */
    suspend operator fun invoke(start: Boolean) {
        if (start) {
            callRepository.openVideoDevice()
        } else {
            callRepository.releaseVideoDevice()
        }
    }
}
