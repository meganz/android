package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import javax.inject.Inject

/**
 * Record that the subscription offer landing screen has just been shown, so it stays hidden until
 * the offer reshow interval has passed.
 *
 * @property getCurrentTimeInMillisUseCase [GetCurrentTimeInMillisUseCase]
 * @property billingRepository             [BillingRepository]
 */
class SetSubscriptionOfferLastShownTimeUseCase @Inject constructor(
    private val getCurrentTimeInMillisUseCase: GetCurrentTimeInMillisUseCase,
    private val billingRepository: BillingRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() =
        billingRepository.setSubscriptionOfferLastShownTime(getCurrentTimeInMillisUseCase())
}
