package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for monitoring the unverified business account state
 */
class MonitorUnverifiedBusinessAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Monitor the state indicating if the current account is an unverified business account
     *
     * @return Flow of Boolean indicating if the account is unverified
     */
    operator fun invoke(): Flow<Boolean> = accountRepository.monitorIsUnverifiedBusinessAccount()
}