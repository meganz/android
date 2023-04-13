package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Get local pricing for specific subscription option
 */
class GetLocalPricingUseCase @Inject constructor(
    private val repository: BillingRepository
) {
    /**
     * Invoke
     *
     * @param sku = String
     * @return LocalPricing?
     */
    operator fun invoke(
        sku: String,
    ) = repository.getLocalPricing(sku)
}