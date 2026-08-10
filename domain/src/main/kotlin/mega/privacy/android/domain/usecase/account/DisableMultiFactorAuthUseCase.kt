package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case that disables Multi-Factor Authentication for the current account.
 */
class DisableMultiFactorAuthUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(pin: String) =
        accountRepository.disableMultiFactorAuth(pin)
}
