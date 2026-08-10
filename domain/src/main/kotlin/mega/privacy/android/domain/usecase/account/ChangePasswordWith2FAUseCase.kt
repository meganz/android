package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case that changes the account password gated by a 2FA PIN.
 */
class ChangePasswordWith2FAUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(newPassword: String, pin: String): Boolean =
        accountRepository.changePasswordWith2FA(newPassword, pin)
}
