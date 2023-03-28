package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Default get account details
 *
 * @property accountsRepository
 */
class GetAccountDetailsUseCase @Inject constructor(
    private val accountsRepository: AccountRepository,
    private val isDatabaseEntryStale: IsDatabaseEntryStale,
){
    suspend operator fun invoke(forceRefresh: Boolean): UserAccount {
        if (forceRefresh || accountsRepository.storageCapacityUsedIsBlank() || isDatabaseEntryStale()) {
            accountsRepository.resetAccountDetailsTimeStamp()
            accountsRepository.requestAccount()
        }
        return accountsRepository.getUserAccount()
    }
}