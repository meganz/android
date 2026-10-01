package mega.privacy.android.domain.usecase.logging

import mega.privacy.android.domain.repository.LoggingRepository
import javax.inject.Inject

/**
 * Get zipped logs use case
 *
 * @property loggingRepository
 * @constructor Create empty Get zipped logs use case
 */
class GetZippedLogsUseCase @Inject constructor(private val loggingRepository: LoggingRepository) {

    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = loggingRepository.compressLogs()
}