package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.AccountRepository
import javax.inject.Inject

class DefaultCanDeleteAccount @Inject constructor(private val accountsRepository: AccountRepository): CanDeleteAccount {
    override fun invoke(): Boolean {
        val userAccount = accountsRepository.getUserAccount()
        return !userAccount.isBusinessAccount || userAccount.isMasterBusinessAccount
    }
}