package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SubscriptionPlan

/**
 * Get list of subscription plans
 */
fun interface GetSubscriptionPlans {
    /**
     * Invoke
     *
     * @return [List<SubscriptionPlan>] if exists.
     */
    suspend operator fun invoke(): List<SubscriptionPlan>
}