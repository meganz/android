package mega.privacy.android.domain.usecase.billing

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Monitor the offer campaigns the current user has dismissed the subscription offer banner for.
 *
 * Offers are hidden per campaign (utqa "mo.c") rather than outright, so dismissing the banner hides
 * every offer of that campaign while leaving the next campaign free to be advertised again.
 *
 * @property billingRepository [BillingRepository]
 */
class MonitorDismissedSubscriptionOfferCampaignsUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) {
    /**
     * Invoke
     *
     * @return [Flow] emitting the campaign ids whose offers should stay hidden for the current user
     */
    operator fun invoke(): Flow<Set<Long>> =
        billingRepository.monitorDismissedSubscriptionOfferCampaigns()
}
