package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case that launches a request to confirm the User's change of Email
 *
 * @property accountRepository [AccountRepository]
 */
class ConfirmChangeEmailUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invocation function
     *
     * @param changeEmailLink The Change Email link
     * @param accountPassword The password of the Account whose email to be changed
     *
     * @return The new Email Address
     */
    suspend operator fun invoke(changeEmailLink: String, accountPassword: String): String =
        accountRepository.confirmChangeEmail(
            changeEmailLink = changeEmailLink,
            accountPassword = accountPassword,
        )
}