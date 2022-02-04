package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.AccountRepository
import javax.inject.Inject

class DefaultRefreshUserAccount @Inject constructor(private val accountsRepository: AccountRepository) : RefreshUserAccount {
    override fun invoke() {
        if(!accountsRepository.hasAccountBeenFetched()) accountsRepository.requestAccount()
    }
}