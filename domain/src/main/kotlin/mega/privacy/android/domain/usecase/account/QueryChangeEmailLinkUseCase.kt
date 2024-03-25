package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case that queries information on a Change Email Link
 *
 * @property accountRepository Contains all Account-related calls
 */
class QueryChangeEmailLinkUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invocation function
     *
     * @param changeEmailLink The Change Email Link to be queried
     * @return The Change Email Link if there are no issues found during the querying process
     */
    suspend operator fun invoke(changeEmailLink: String): String =
        accountRepository.queryChangeEmailLink(changeEmailLink)
}