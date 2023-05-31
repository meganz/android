package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Monitor refresh session use case
 *
 */
class MonitorRefreshSessionUseCase @Inject constructor(private val accountRepository: AccountRepository) {
    /**
     * Invoke
     *
     */
    operator fun invoke() = accountRepository.monitorRefreshSession()
}