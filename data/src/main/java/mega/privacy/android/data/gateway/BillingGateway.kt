package mega.privacy.android.data.gateway

import android.app.Activity
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.ExternalContentLinkResult
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.exception.ProductNotFoundException

/**
 * Billing gateway interface
 *
 */
internal interface BillingGateway {
    /**
     * Query purchase
     *
     */
    suspend fun queryPurchase(): List<MegaPurchase>

    /**
     * Query product
     *
     */
    suspend fun querySkus(skus: List<String>): List<MegaSku>

    /**
     * Get country code
     *
     */
    suspend fun getCountryCode(): String?

    /**
     * Monitor billing event
     * it will emit when calling queryProductDetails and queryPurchasesAsync
     */
    fun monitorBillingEvent(): Flow<BillingEvent>

    /**
     * Launch a purchase flow.
     */
    @Throws(ProductNotFoundException::class)
    suspend fun launchPurchaseFlow(activity: Activity, productId: String, offerId: String?)

    /**
     * Check if subscription feature is available on this device (e.g. Google Play supports subscriptions).
     *
     * @return [Boolean] true if subscriptions are supported, false otherwise
     */
    suspend fun isSubscriptionFeatureAvailable(): Boolean

    /**
     * Check if external content links billing program is available.
     *
     * @return [Boolean] true if external content links are available, false otherwise
     */
    suspend fun isExternalContentLinkAvailable(): Boolean

    /**
     * Create billing program reporting details to get external transaction token.
     *
     * @return [String]? The external transaction token, or null if not available
     */
    suspend fun createExternalContentLinkReportingDetails(): String?

    /**
     * Launch external link using Google Play Billing Library's external content links API.
     *
     * @param activity The activity to launch from
     * @param linkUri The URI of the external website
     * @return [ExternalContentLinkResult] The result of the operation (Success, Cancelled, or Failed)
     */
    suspend fun launchExternalContentLink(
        activity: Activity,
        linkUri: Uri,
    ): ExternalContentLinkResult
}

