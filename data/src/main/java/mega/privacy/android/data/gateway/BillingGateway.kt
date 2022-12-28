package mega.privacy.android.data.gateway

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.billing.BillingEvent
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
    suspend fun querySkus(): List<MegaSku>

    /**
     * Monitor billing event
     * it will emit when calling queryProductDetails and queryPurchasesAsync
     */
    fun monitorBillingEvent(): Flow<BillingEvent>

    /**
     * Launch a purchase flow.
     */
    @Throws(ProductNotFoundException::class)
    suspend fun launchPurchaseFlow(activity: Activity, productId: String)
}