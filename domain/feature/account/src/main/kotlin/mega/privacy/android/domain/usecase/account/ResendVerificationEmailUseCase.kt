package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case to resend verification email in case the user didn't receive it.
 */
class ResendVerificationEmailUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() =
        accountRepository.resendVerificationEmail()
}