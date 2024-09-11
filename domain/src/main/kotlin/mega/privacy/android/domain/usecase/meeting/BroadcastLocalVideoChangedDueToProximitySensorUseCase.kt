package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject


/**
 * Use case for broadcasting when local video has changed due to proximity sensor.
 */
class BroadcastLocalVideoChangedDueToProximitySensorUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param isVideoOn    True, if video is on. False, if it's off
     */
    suspend operator fun invoke(isVideoOn: Boolean) =
        callRepository.broadcastLocalVideoChangedDueToProximitySensor(isVideoOn)
}