package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.LocalPricing
import mega.privacy.android.domain.entity.billing.PaymentMethodFlags


/**
 * Billing repository
 */
interface BillingRepository {
    /**
     * Get local pricing for specific option available at Google Store
     *
     * @param sku [String] specific string for each subscription option
     * @return LocalPricing?
     */
    suspend fun getLocalPricing(sku: String): LocalPricing?

    /**
     * Get payment method
     *
     */
    suspend fun getPaymentMethod(clearCache: Boolean): PaymentMethodFlags
}