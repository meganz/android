package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Monitor My Account Update Use Case
 */
class MonitorMyAccountUpdateUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() = accountRepository.monitorMyAccountUpdate()
}