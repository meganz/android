package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Broadcast refresh session use case
 *
 */
class BroadcastRefreshSessionUseCase @Inject constructor(private val accountRepository: AccountRepository) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = accountRepository.broadcastRefreshSession()
}