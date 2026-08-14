package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Persist that the current user has dismissed the subscription offer banner for the given campaign.
 *
 * Every offer of that campaign stays hidden from then on, while the offers of the next campaign are
 * advertised again.
 *
 * @property billingRepository [BillingRepository]
 */
class DismissSubscriptionOfferCampaignUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) {
    /**
     * Invoke
     *
     * @param campaignId the campaign whose offers should stay hidden
     */
    suspend operator fun invoke(campaignId: Long) =
        billingRepository.addDismissedSubscriptionOfferCampaign(campaignId)
}
