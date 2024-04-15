package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * A use case to cancel the registration process of a generated sign-up link
 */
class CancelCreateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invocation method
     */
    suspend operator fun invoke() {
        accountRepository.cancelCreateAccount()
    }
}
