package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Monitor if an account security upgrade in required
 */
class MonitorSecurityUpgradeInAppUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @return flow with boolean true if a security upgrade is required, false otherwise
     */
    operator fun invoke(): Flow<Boolean> =
        accountRepository.monitorSecurityUpgrade()
}
