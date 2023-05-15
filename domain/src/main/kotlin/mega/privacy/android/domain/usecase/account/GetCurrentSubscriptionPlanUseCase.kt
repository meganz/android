package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Get current subscription plan
 */
class GetCurrentSubscriptionPlanUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @return [AccountType]
     */
    suspend operator fun invoke(): AccountType? =
        repository::getUserAccount.invoke().accountTypeIdentifier
}