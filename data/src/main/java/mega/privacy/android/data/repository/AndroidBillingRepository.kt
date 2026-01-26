package mega.privacy.android.data.repository

import android.app.Activity
import android.net.Uri
import mega.privacy.android.domain.entity.billing.ExternalContentLinkResult
import mega.privacy.android.domain.entity.payment.UpgradeSource

/**
 * Android billing repository
 *
 * We can not define Activity in the domain layer, keeping it in the data layer
 * The app module create UseCase call to this
 */
interface AndroidBillingRepository {
    /**
     * Launch a purchase flow.
     */
    suspend fun launchPurchaseFlow(
        activity: Activity,
        source: UpgradeSource,
        productId: String,
        offerId: String?,
    )

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
