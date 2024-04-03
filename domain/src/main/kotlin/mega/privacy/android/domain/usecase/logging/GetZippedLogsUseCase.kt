package mega.privacy.android.domain.usecase.logging

import mega.privacy.android.domain.repository.LoggingRepository
import javax.inject.Inject

class GetZippedLogsUseCase @Inject constructor(private val loggingRepository: LoggingRepository) {

    suspend operator fun invoke() = loggingRepository.compressLogs()
}