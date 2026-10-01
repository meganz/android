package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Persist that the current user has dismissed the subscription offer banner on the Menu screen for
 * the given campaign.
 *
 * Tracked separately from [DismissSubscriptionOfferCampaignUseCase], so dismissing the banner on the
 * Menu screen leaves it visible on the Home carousel.
 *
 * @property billingRepository [BillingRepository]
 */
class DismissSubscriptionOfferMenuCampaignUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) {
    /**
     * Invoke
     *
     * @param campaignId the campaign whose offers should stay hidden on the Menu screen
     */
    suspend operator fun invoke(campaignId: Long) =
        billingRepository.addDismissedSubscriptionOfferMenuCampaign(campaignId)
}
