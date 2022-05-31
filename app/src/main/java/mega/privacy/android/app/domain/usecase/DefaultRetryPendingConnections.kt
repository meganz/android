package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Default [RetryPendingConnections] implementation.
 *
 * @property accountRepository [AccountRepository]
 */
class DefaultRetryPendingConnections @Inject constructor(
    private val accountRepository: AccountRepository,
) : RetryPendingConnections {

    override fun invoke(disconnect: Boolean) {
        accountRepository.retryPendingConnections(disconnect)
    }
}