package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Checks if user session exists.
 */
class GetSessionUseCase @Inject constructor(private val accountRepository: AccountRepository) {
    /**
     * Invoke
     *
     * @return session if exists.
     */
    suspend operator fun invoke() = accountRepository.getSession()
}