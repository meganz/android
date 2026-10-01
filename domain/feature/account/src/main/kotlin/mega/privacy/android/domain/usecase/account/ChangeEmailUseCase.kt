package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to change the email registered to the account
 */
class ChangeEmailUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invocation function
     *
     * @param email the new email
     * @return The email returned from the SDK
     */
    suspend operator fun invoke(email: String) = accountRepository.changeEmail(email)
}