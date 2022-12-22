package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.LocalPricing
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.entity.billing.PaymentMethodFlags
import mega.privacy.android.domain.entity.billing.Pricing


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

    /**
     * Get pricing
     *
     */
    suspend fun getPricing(clearCache: Boolean): Pricing

    /**
     * Get credit card query subscriptions
     *
     */
    suspend fun getNumberOfSubscription(clearCache: Boolean): Long

    /**
     * Query purchase
     *
     */
    suspend fun queryPurchase(): List<MegaPurchase>

    /**
     * Query skus
     *
     */
    suspend fun querySkus(): List<MegaSku>

    /**
     * Disconnect
     *
     */
    suspend fun disconnect()

    /**
     * Monitor billing event
     *
     */
    fun monitorBillingEvent(): Flow<BillingEvent>
}