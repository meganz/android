package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case that queries information on an Account Cancellation Link
 *
 * @property accountRepository Contains all Account-related calls
 */
class QueryCancelLinkUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invocation function
     *
     * @param accountCancellationLink The Account Cancellation Link to be queried
     * @return The Account Cancellation Link when no issues are found
     */
    suspend operator fun invoke(accountCancellationLink: String): String =
        accountRepository.queryCancelLink(accountCancellationLink)
}