package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case that requests a delete (cancel) account link gated by a 2FA PIN.
 */
class RequestDeleteAccountLinkWith2FAUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(pin: String) =
        accountRepository.requestDeleteAccountLinkWith2FA(pin)
}
