package mega.privacy.android.app.usecase.billing

import android.app.Activity

/**
 * Launch purchase flow Android UseCase
 *
 */
fun interface LaunchPurchaseFlow {
    suspend operator fun invoke(activity: Activity, productId: String)
}