package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for resetting account info.
 */
class ResetAccountInfoUseCase @Inject constructor(private val accountRepository: AccountRepository) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() = accountRepository.resetAccountInfo()
}