package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to kill all other active Sessions except the current Session
 *
 * @property accountRepository [AccountRepository]
 */
class KillOtherSessionsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invocation function
     */
    suspend operator fun invoke() = accountRepository.killOtherSessions()
}