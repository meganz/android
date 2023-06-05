package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Is billing available
 *
 */
class IsBillingAvailableUseCase @Inject constructor(
    private val repository: BillingRepository
) {
    /**
     * Invoke
     *
     * @return [Boolean] true if skus from billing is not empty otherwise false
     */
    operator fun invoke() = repository.isBillingAvailable()
}