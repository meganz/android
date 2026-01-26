package mega.privacy.android.feature.payment.domain

import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Create external content link reporting details to get external transaction token.
 *
 * This use case creates billing program reporting details which returns an external
 * transaction token that can be used for external content link purchases.
 */
class CreateExternalContentLinkTokenUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) {
    /**
     * Invoke - Creates billing program reporting details and returns the token.
     *
     * @return [String]? The external transaction token, or null if not available
     */
    suspend operator fun invoke(): String? =
        billingRepository.createExternalContentLinkReportingDetails()
}
