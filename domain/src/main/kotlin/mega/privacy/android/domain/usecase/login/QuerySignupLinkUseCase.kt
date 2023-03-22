package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for querying signup links.
 */
class QuerySignupLinkUseCase @Inject constructor(private val accountRepository: AccountRepository) {

    /**
     * Invoke.
     *
     * @param link Link to query.
     * @return The email related to the link.
     */
    suspend operator fun invoke(link: String) = accountRepository.querySignupLink(link)
}