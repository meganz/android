package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Check if subscription feature is available on this device.
 *
 * This indicates whether the device supports in-app subscriptions (e.g. Google Play
 * subscriptions are available and not restricted). When false, the app may show
 * alternative upgrade options such as external content links.
 *
 * @property billingRepository [BillingRepository]
 */
class IsSubscriptionFeatureAvailableUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) {
    /**
     * Invoke
     *
     * @return [Boolean] true if subscription feature is available, false otherwise
     */
    suspend operator fun invoke(): Boolean = billingRepository.isSubscriptionFeatureAvailable()
}
