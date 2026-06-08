package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.LoggingRepository
import javax.inject.Inject

/**
 * Initialise the logging subsystem at app startup.
 *
 * Trees are planted as a side-effect of injecting [LoggingRepository], but the
 * underlying Logback configuration must be applied before any log entry can
 * actually reach disk — that's what this use case does.
 */
class InitialiseLoggingUseCase @Inject constructor(
    private val loggingRepository: LoggingRepository,
) {
    suspend operator fun invoke() {
        loggingRepository.initialise()
    }
}
