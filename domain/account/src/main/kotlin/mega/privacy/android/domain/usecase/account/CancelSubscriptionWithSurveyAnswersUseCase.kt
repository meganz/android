package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Use case to provide API with cancellation survey answers
 */
class CancelSubscriptionWithSurveyAnswersUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) {
    /**
     * Provides API with cancellation survey answers
     */
    suspend operator fun invoke(reason: String, subscriptionId: String, canContact: Int) =
        billingRepository.cancelSubscriptionWithSurveyAnswers(reason, subscriptionId, canContact)
}