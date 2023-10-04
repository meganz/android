package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Print recovery key use case
 */
class GetPrintRecoveryKeyFileUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = accountRepository.getRecoveryKeyFile()
}