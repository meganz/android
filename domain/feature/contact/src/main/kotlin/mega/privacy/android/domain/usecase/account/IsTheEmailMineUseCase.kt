package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * A use case to check if the given email is the current user's email.
 *
 * @property accountRepository The account-related repository.
 */
class IsTheEmailMineUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invocation method.
     *
     * @param email The email that needs to be checked.
     * @return Boolean. Whether the email matches that of the current user.
     */
    suspend operator fun invoke(email: String): Boolean =
        accountRepository.getCurrentUser()?.email == email
}
