package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case that launches a request to confirm an Account cancellation
 *
 * @property accountRepository [AccountRepository]
 */
class ConfirmCancelAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invocation function
     *
     * @param cancellationLink The Account cancellation link
     * @param accountPassword The password of the Account to be cancelled
     */
    suspend operator fun invoke(cancellationLink: String, accountPassword: String) =
        accountRepository.confirmCancelAccount(
            cancellationLink = cancellationLink,
            accountPassword = accountPassword,
        )
}