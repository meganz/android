package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case to reset the account details timestamp.
 */
class ResetAccountDetailsTimeStampUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Invoke the use case
     */
    suspend operator fun invoke() {
        accountRepository.resetAccountDetailsTimeStamp()
    }
}