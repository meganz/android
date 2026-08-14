package mega.privacy.android.domain.usecase.billing

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Monitor the offer campaigns the current user has dismissed the subscription offer banner on the
 * Menu screen for.
 *
 * Tracked separately from [MonitorDismissedSubscriptionOfferCampaignsUseCase], so dismissing the
 * banner on the Home carousel leaves it visible on the Menu screen and vice versa.
 *
 * @property billingRepository [BillingRepository]
 */
class MonitorDismissedSubscriptionOfferMenuCampaignsUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) {
    /**
     * Invoke
     *
     * @return [Flow] emitting the campaign ids whose offers should stay hidden on the Menu screen
     */
    operator fun invoke(): Flow<Set<Long>> =
        billingRepository.monitorDismissedSubscriptionOfferMenuCampaigns()
}
