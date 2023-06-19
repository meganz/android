package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for monitoring blocked account.
 */
class MonitorAccountBlockedUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke.
     *
     * @return Flow of [AccountBlockedDetail]
     */
    operator fun invoke() = accountRepository.monitorAccountBlocked()
}