package mega.privacy.android.domain.usecase.environment

import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Get the number of available processors
 */
class GetAvailableProcessorsUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
) {

    /**
     * Invoke
     */
    operator fun invoke(): Int = environmentRepository.availableProcessors()
}
