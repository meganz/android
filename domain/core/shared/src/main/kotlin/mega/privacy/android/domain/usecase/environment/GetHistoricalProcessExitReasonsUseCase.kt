package mega.privacy.android.domain.usecase.environment

import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Use case to get historical process exit reasons
 */
class GetHistoricalProcessExitReasonsUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository
) {
    /**
     * Invoke function to get historical process exit reasons
     */
    suspend operator fun invoke() {
        environmentRepository.getHistoricalProcessExitReasons()
    }
}