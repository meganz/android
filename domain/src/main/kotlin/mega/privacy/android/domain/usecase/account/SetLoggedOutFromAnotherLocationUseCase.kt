package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for setting the state indicating if the user session has been logged out from another location.
 */
class SetLoggedOutFromAnotherLocationUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke.
     *
     * @param isLoggedOut Boolean indicating if the session is logged out.
     */
    suspend operator fun invoke(isLoggedOut: Boolean) {
        accountRepository.setLoggedOutFromAnotherLocation(isLoggedOut)
    }
}