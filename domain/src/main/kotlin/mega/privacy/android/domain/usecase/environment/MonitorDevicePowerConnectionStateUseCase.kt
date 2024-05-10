package mega.privacy.android.domain.usecase.environment

import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Use Case that monitors the Device Power Connection State
 *
 * @param environmentRepository Repository containing all Android OS related functions
 */
class MonitorDevicePowerConnectionStateUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
) {
    /**
     * Invocation function
     *
     * @return a Flow that observes and returns the Device Power Connection State
     */
    operator fun invoke() = environmentRepository.monitorDevicePowerConnectionState()
}