package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Check Recovery Key Use Case
 *
 * Use case for checking the recovery key.
 */
class CheckRecoveryKeyUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @param link the recovery key link
     * @param recoveryKey the recovery key
     */
    suspend operator fun invoke(link: String, recoveryKey: String) {
        accountRepository.checkRecoveryKey(link, recoveryKey)
    }
}