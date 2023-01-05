package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.entity.billing.MegaPurchase

/**
 * Get active subscription from local cache
 *
 */
fun interface GetActiveSubscription {
    /**
     * Invoke
     *
     */
    operator fun invoke(): MegaPurchase?
}