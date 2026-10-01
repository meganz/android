package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Get device's current time in milli second from System
 */
class GetDeviceCurrentTimeUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
) {
    /**
     * invoke
     */
    operator fun invoke() = environmentRepository.now
}
