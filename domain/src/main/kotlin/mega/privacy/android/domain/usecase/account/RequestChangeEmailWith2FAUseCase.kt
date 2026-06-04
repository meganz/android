package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case that requests a change-email confirmation link gated by a 2FA PIN.
 */
class RequestChangeEmailWith2FAUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(newEmail: String, pin: String) =
        accountRepository.requestChangeEmailWith2FA(newEmail, pin)
}
