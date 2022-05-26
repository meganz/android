package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.UserCredentials
import mega.privacy.android.app.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Default [GetCredentials] implementation.
 *
 * @property accountRepository [AccountRepository]
 */
class DefaultGetCredentials @Inject constructor(
    private val accountRepository: AccountRepository
) : GetCredentials {

    override suspend fun invoke(): UserCredentials? = accountRepository.getCredentials()
}