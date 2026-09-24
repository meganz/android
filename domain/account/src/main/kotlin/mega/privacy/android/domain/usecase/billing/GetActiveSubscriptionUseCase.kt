package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Get active subscription from local cache
 *
 */
class GetActiveSubscriptionUseCase @Inject constructor(
    private val repository: BillingRepository,
) {
    /**
     * Invoke
     *
     * @return [MegaPurchase?]
     */
    operator fun invoke() = repository.getActiveSubscription()
}