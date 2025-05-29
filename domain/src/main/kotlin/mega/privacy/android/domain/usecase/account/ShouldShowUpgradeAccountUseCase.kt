package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case to determine if the upgrade account screen should be shown.
 */
class ShouldShowUpgradeAccountUseCase @Inject constructor(
    private val getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase,
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @return true if the upgrade account screen should be shown, false otherwise
     */
    suspend operator fun invoke(): Boolean {
        if (!accountRepository.hasUserLoggedInBefore()) {
            val accountDetail =
                getSpecificAccountDetailUseCase(storage = false, transfer = false, pro = true)
            accountRepository.getLoggedInUserId()?.let { userId ->
                accountRepository.addLoggedInUserHandle(userId.id)
            }
            return accountDetail.levelDetail?.accountType == AccountType.FREE
        }
        return false
    }
}