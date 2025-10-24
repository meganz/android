package mega.privacy.android.feature.payment.domain

import android.app.Activity
import mega.privacy.android.data.repository.AndroidBillingRepository
import javax.inject.Inject

class LaunchPurchaseFlowUseCase @Inject constructor(
    private val androidBillingRepository: AndroidBillingRepository,
) {
    suspend operator fun invoke(activity: Activity, productId: String, offerId: String?) {
        androidBillingRepository.launchPurchaseFlow(activity, productId, offerId)
    }
}