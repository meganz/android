package mega.privacy.android.domain.usecase.logging

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.LoggingRepository
import javax.inject.Inject

/**
 * Use case to check if the chat logs are enabled
 */
class AreChatLogsEnabledUseCase @Inject constructor(
    private val repository: LoggingRepository,
) {

    /**
     * Invocation method.
     *
     * @return Flow of boolean. True if enabled else false.
     */
    operator fun invoke(): Flow<Boolean> = repository.isChatLoggingEnabled()
}
