package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.billing.Pricing

/**
 * Get pricing
 *
 */
fun interface GetPricing {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(forceRefresh: Boolean): Pricing
}