package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.exception.MegaException
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
     *
     * @return The corresponding email
     */
    @Throws(MegaException::class)
    suspend operator fun invoke() = accountRepository.cancelCreateAccount()
}
