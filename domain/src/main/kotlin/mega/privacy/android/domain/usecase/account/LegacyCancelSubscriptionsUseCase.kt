package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Use case to cancel card subscriptions.
 *
 * @property billingRepository Repository to manage all billing related requests.
 */
class LegacyCancelSubscriptionsUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) {

    /**
     * Launches a request to cancel card subscriptions.
     *
     * @param feedback Message typed as reason to cancel subscriptions.
     * @return True if the request finished with success, error if not.
     */
    suspend operator fun invoke(feedback: String?): Boolean =
        billingRepository.legacyCancelSubscriptions(feedback)

}