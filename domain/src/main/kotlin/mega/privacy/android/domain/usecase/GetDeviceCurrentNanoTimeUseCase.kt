package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Get device's current nano time from System
 */
class GetDeviceCurrentNanoTimeUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
) {
    /**
     * invoke
     */
    operator fun invoke() = environmentRepository.nanoTime
}
