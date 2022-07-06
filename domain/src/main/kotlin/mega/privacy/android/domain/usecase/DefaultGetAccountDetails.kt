package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Default get account details
 *
 * @property accountsRepository
 */
class DefaultGetAccountDetails @Inject constructor(private val accountsRepository: AccountRepository) :
    GetAccountDetails {
    override suspend fun invoke(forceRefresh: Boolean): UserAccount {
        if (accountsRepository.isAccountDataStale() || forceRefresh) accountsRepository.requestAccount()
        return accountsRepository.getUserAccount()
    }
}