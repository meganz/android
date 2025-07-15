package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Monitor billing event
 * listen purchase updated when user process the purchase
 */
class MonitorBillingEventUseCase @Inject constructor(private val billingRepository: BillingRepository) {
    operator fun invoke() = billingRepository.monitorBillingEvent()
}