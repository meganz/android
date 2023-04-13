package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.entity.PaymentMethod
import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject


/**
 * Get current payment available through the store
 */
class GetCurrentPaymentUseCase @Inject constructor(
    private val repository: BillingRepository,
) {
    /**
     * Invoke
     *
     * @return [PaymentMethod]
     */
    suspend operator fun invoke() = repository.getCurrentPaymentMethod()
}