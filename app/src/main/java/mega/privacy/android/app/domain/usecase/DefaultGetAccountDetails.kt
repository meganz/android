package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.UserAccount
import mega.privacy.android.app.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Default get account details
 *
 * @property accountsRepository
 */
class DefaultGetAccountDetails @Inject constructor(private val accountsRepository: AccountRepository): GetAccountDetails {
    override fun invoke(forceRefresh: Boolean): UserAccount {
        if (accountsRepository.isAccountDataStale() || forceRefresh) accountsRepository.requestAccount()
        return accountsRepository.getUserAccount()
    }
}