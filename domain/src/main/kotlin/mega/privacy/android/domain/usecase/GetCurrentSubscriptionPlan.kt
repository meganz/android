package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.AccountType

/**
 * Get current subscription plan
 */
fun interface GetCurrentSubscriptionPlan {
    /**
     * Invoke
     *
     * @return [AccountType]
     */
    suspend operator fun invoke(): AccountType?
}