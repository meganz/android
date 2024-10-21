package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Get current storage state of the account
 */
class GetCurrentStorageStateUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     *  Invoke function
     *
     * @return [StorageState] object
     */
    suspend operator fun invoke(): StorageState = accountRepository.getStorageState()
}