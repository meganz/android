package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Check whether the subscription offer landing screen should be shown on app launch.
 *
 * The screen is only shown when a plan currently carries a mobile offer
 * ([GetRecommendedSubscriptionWithOfferUseCase]). Once shown, it is not shown again until the
 * offer's reshow interval (utqa "mo.r") has elapsed; an offer without a reshow interval is only
 * ever shown once.
 *
 * @property getRecommendedSubscriptionWithOfferUseCase [GetRecommendedSubscriptionWithOfferUseCase]
 * @property getCurrentTimeInMillisUseCase              [GetCurrentTimeInMillisUseCase]
 * @property billingRepository                          [BillingRepository]
 */
class ShouldShowSubscriptionOfferUseCase @Inject constructor(
    private val getRecommendedSubscriptionWithOfferUseCase: GetRecommendedSubscriptionWithOfferUseCase,
    private val getCurrentTimeInMillisUseCase: GetCurrentTimeInMillisUseCase,
    private val billingRepository: BillingRepository,
) {
    /**
     * Invoke
     *
     * @return true when the offer screen should be shown
     */
    suspend operator fun invoke(): Boolean {
        val offer = getRecommendedSubscriptionWithOfferUseCase() ?: return false
        val lastShownTime = billingRepository.getSubscriptionOfferLastShownTime() ?: return true
        val reshowInterval = offer.subscription.offerReshowInterval ?: return false
        return getCurrentTimeInMillisUseCase() - lastShownTime >=
                reshowInterval.seconds.inWholeMilliseconds
    }
}
