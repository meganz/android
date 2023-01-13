package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Default IsUserLoggedIn
 *
 * @property accountRepository
 */
class DefaultIsUserLoggedIn @Inject constructor(private val accountRepository: AccountRepository) :
    IsUserLoggedIn {
    override suspend fun invoke() = accountRepository.isUserLoggedIn()
}
