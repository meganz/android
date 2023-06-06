package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Get payment method use-case
 *
 */
class GetPaymentMethodUseCase @Inject constructor(
    private val repository: BillingRepository,
) {
    /**
     * Invoke
     *
     * @param forceRefresh [Boolean] if true the cache will be cleared before getting the payment methods
     * @return PaymentMethodFlags
     */
    suspend operator fun invoke(forceRefresh: Boolean) = repository.getPaymentMethod(forceRefresh)
}