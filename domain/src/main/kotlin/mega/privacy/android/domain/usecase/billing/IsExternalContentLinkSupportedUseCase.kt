package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Check if external content links billing program is available.
 *
 * External content links allow apps to direct users to external websites for purchases
 * while still using Google Play Billing for verification.
 *
 * @property billingRepository [BillingRepository]
 */
class IsExternalContentLinkSupportedUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) {
    /**
     * Invoke
     *
     * @return [Boolean] true if external content links are available, false otherwise
     */
    suspend operator fun invoke(): Boolean = billingRepository.isExternalContentLinkAvailable()
}

