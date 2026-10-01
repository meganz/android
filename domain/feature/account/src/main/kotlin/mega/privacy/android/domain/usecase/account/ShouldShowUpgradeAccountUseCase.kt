package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.AccountType
import javax.inject.Inject

/**
 * Use case to determine if the upgrade account screen should be shown.
 */
class ShouldShowUpgradeAccountUseCase @Inject constructor(
    private val getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase,
) {
    /**
     * Invoke
     *
     * @return true if the upgrade account screen should be shown, false otherwise
     */
    suspend operator fun invoke(): Boolean {
        val accountDetail =
            getSpecificAccountDetailUseCase(storage = false, transfer = false, pro = true)
        return accountDetail.levelDetail?.accountType == AccountType.FREE
    }
}