package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for monitoring when the local video is changed.
 */
class MonitorLocalVideoChangedDueToProximitySensorUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @return Flow of Boolean
     */
    operator fun invoke() = callRepository.monitorLocalVideoChangedDueToProximitySensor()
}