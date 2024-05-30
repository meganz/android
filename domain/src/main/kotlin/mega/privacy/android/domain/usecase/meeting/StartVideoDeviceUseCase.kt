package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest
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
     * @param enable  True to enable the video device, false to stop.
     */
    suspend operator fun invoke(enable: Boolean): ChatRequest =
        when {
            enable -> callRepository.openVideoDevice()
            else -> callRepository.releaseVideoDevice()
        }
}
