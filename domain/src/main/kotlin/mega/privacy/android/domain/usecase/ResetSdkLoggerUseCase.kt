package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.LoggingRepository
import javax.inject.Inject

/**
 * Reset sdk logger
 *
 */
class ResetSdkLoggerUseCase @Inject constructor(private val loggingRepository: LoggingRepository) {
    /**
     * Invoke
     *
     */
    operator fun invoke() {
        loggingRepository.resetSdkLogging()
    }
}
