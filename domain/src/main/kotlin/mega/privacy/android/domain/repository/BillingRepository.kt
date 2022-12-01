package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.billing.PaymentMethodFlags


/**
 * Billing repository
 */
interface BillingRepository {
    /**
     * Get local price and local currency for specific plan available at Google Store or Huawei Store
     *
     * @param sku [String] specific string for each subscription plan
     * @return formatted local price string
     */
    suspend fun getLocalPricing(sku: String): MegaSku?

    /**
     * Get payment method
     *
     */
    suspend fun getPaymentMethod(clearCache: Boolean): PaymentMethodFlags
}