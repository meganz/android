package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Retry pending connections use case.
 */
class RetryPendingConnectionsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke.
     *
     * @param disconnect True if should disconnect megaChatApi, false otherwise.
     */
    suspend operator fun invoke(disconnect: Boolean) {
        accountRepository.retryPendingConnections()
        accountRepository.retryChatPendingConnections(disconnect)
    }
}
