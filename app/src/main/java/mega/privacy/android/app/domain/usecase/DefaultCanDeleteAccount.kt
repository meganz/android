package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.UserAccount
import mega.privacy.android.app.domain.repository.AccountRepository
import javax.inject.Inject

class DefaultCanDeleteAccount @Inject constructor(): CanDeleteAccount {
    override fun invoke(account: UserAccount): Boolean {
        return !account.isBusinessAccount || account.isMasterBusinessAccount
    }
}