package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Get User Account type use case
 */
class GetAccountTypeUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(): AccountType = accountRepository.getAccountType()
}