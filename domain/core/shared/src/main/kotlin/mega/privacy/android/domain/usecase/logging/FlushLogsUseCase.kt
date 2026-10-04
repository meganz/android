package mega.privacy.android.domain.usecase.logging

import mega.privacy.android.domain.repository.LoggingRepository
import javax.inject.Inject

/**
 * Force buffered log data to be written out.
 *
 * Invoked at app-lifecycle boundaries (e.g. when the app enters the background)
 * to reduce the amount of log data lost if the OS kills the process before the
 * next automatic flush.
 */
class FlushLogsUseCase @Inject constructor(
    private val loggingRepository: LoggingRepository,
) {
    suspend operator fun invoke() = loggingRepository.flush()
}
