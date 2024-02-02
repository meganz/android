package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case that checks if Multi-Factor Authentication has been enabled on the current Account or not
 *
 * @property accountRepository [AccountRepository]
 */
class IsMultiFactorAuthEnabledUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invocation function
     *
     * @return true if Multi-Factor Authentication has been enabled on the current Account
     */
    suspend operator fun invoke() = accountRepository.isMultiFactorAuthEnabled()
}