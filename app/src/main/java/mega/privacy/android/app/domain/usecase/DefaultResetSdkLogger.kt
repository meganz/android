package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.LoggingRepository
import javax.inject.Inject

/**
 * Default reset sdk logger
 *
 * @property loggingRepository
 */
class DefaultResetSdkLogger @Inject constructor(private val loggingRepository: LoggingRepository) : ResetSdkLogger {
    override fun invoke() {
        loggingRepository.resetSdkLogging()
    }
}