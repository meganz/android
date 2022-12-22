package mega.privacy.android.data.repository

import android.app.Activity

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
    suspend fun launchPurchaseFlow(activity: Activity, productId: String)
}