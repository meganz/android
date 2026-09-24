package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.AccountType
import javax.inject.Inject

/**
 * Use case to check if the account is a PRO account
 */
class IsProAccountUseCase @Inject constructor(
    private val getAccountTypeUseCase: GetAccountTypeUseCase,
) {

    /**
     * Invokes the use case
     */
    suspend operator fun invoke(): Boolean {
        val accountType = getAccountTypeUseCase()
        return accountType != AccountType.FREE
    }
}