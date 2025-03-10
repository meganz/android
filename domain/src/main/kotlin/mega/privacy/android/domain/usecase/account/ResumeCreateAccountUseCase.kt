package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for resuming the account creation process.
 */
class ResumeCreateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Invoke the use case.
     *
     * @param session The session string.
     */
    suspend operator fun invoke(session: String) {
        return accountRepository.resumeCreateAccount(session)
    }
}