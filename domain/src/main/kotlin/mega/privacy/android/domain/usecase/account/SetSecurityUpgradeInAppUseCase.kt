package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Set boolean flag in app to inform that an account security upgrade is required
 *
 */
class SetSecurityUpgradeInAppUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @param isSecurityUpgrade true if security upgrade is required, false otherwise
     */
    suspend operator fun invoke(isSecurityUpgrade: Boolean) =
        accountRepository.setUpgradeSecurity(isSecurityUpgrade)
}
