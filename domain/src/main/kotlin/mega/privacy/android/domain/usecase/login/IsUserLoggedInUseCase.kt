package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case that checks if the User is logged in to the app
 */
class IsUserLoggedInUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invocation function.
     *
     * @return true if the User is logged in
     */
    suspend operator fun invoke() = accountRepository.isUserLoggedIn()
}