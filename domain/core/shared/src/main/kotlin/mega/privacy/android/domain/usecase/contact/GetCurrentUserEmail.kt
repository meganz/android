package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Get current user email
 *
 */
class GetCurrentUserEmail @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(forceRefresh: Boolean = true) = accountRepository.getAccountEmail(forceRefresh)
}