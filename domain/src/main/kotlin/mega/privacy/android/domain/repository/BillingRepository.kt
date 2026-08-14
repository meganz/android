package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.LocalPricing
import mega.privacy.android.domain.entity.PaymentMethod
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
    fun getLocalPricing(sku: String): LocalPricing?

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
    suspend fun querySkus(skus: List<String>): List<MegaSku>

    /**
     * Monitor billing event
     *
     */
    fun monitorBillingEvent(): Flow<BillingEvent>

    /**
     * Get current payment method, if current account has paid subscription, or null
     */
    suspend fun getCurrentPaymentMethod(): PaymentMethod?

    /**
     * Check if user can pay through the app
     */
    fun isBillingAvailable(): Boolean

    /**
     * Get active subscription
     *
     */
    fun getActiveSubscription(): MegaPurchase?

    /**
     * Clear cache
     *
     */
    fun clearCache()

    /**
     * Get the credit card subscriptions of the account
     */
    suspend fun legacyCancelSubscriptions(feedback: String?): Boolean

    /**
     * Get the billing country code
     *
     * @return the billing country code as a String
     */
    suspend fun getBillingCountryCode(): String?

    /**
     * Check if subscription feature is available on this device (e.g. Google Play supports subscriptions).
     *
     * @return [Boolean] true if subscriptions are supported, false otherwise
     */
    suspend fun isSubscriptionFeatureAvailable(): Boolean

    /**
     * Provide API with cancellation survey answers
     */
    suspend fun cancelSubscriptionWithSurveyAnswers(
        reason: String,
        subscriptionId: String,
        canContact: Int,
    )

    /**
     * Monitor the offer campaigns the current user has dismissed the subscription offer banner for.
     * The preference is stored per account, so dismissing the banner does not hide it for other
     * logged in users.
     */
    fun monitorDismissedSubscriptionOfferCampaigns(): Flow<Set<Long>>

    /**
     * Persist that the current user has dismissed the subscription offer banner for the given
     * campaign, so every offer of that campaign stays hidden on the next app launch.
     *
     * @param campaignId the campaign whose offers should stay hidden
     */
    suspend fun addDismissedSubscriptionOfferCampaign(campaignId: Long)

    /**
     * Monitor the offer campaigns the current user has dismissed the subscription offer banner on
     * the Menu screen for. Tracked separately from [monitorDismissedSubscriptionOfferCampaigns], so
     * dismissing the banner on one surface leaves it visible on the other.
     */
    fun monitorDismissedSubscriptionOfferMenuCampaigns(): Flow<Set<Long>>

    /**
     * Persist that the current user has dismissed the subscription offer banner on the Menu screen
     * for the given campaign, so every offer of that campaign stays hidden on the next app launch.
     *
     * @param campaignId the campaign whose offers should stay hidden
     */
    suspend fun addDismissedSubscriptionOfferMenuCampaign(campaignId: Long)

    /**
     * Get when the subscription offer screen was last shown to the current user. The preference is
     * stored per account.
     *
     * @return the time in milliseconds, or null when the screen has never been shown
     */
    suspend fun getSubscriptionOfferLastShownTime(): Long?

    /**
     * Persist when the subscription offer screen was last shown to the current user, so it is not
     * shown again until the offer reshow interval has passed.
     *
     * @param timeInMillis the time the screen was shown
     */
    suspend fun setSubscriptionOfferLastShownTime(timeInMillis: Long)

}

