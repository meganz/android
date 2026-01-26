package mega.privacy.android.feature.payment.usecase

import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.usecase.login.GetSessionTransferURLUseCase
import javax.inject.Inject

/**
 * Use case to generate the URL for external purchase checkout.
 *
 * This use case constructs a URL that includes:
 * - Product ID for the subscription plan
 * - User agent override (UAO) parameter with app version information
 * - Optional months parameter for billing period
 * - Optional external transaction token from Google Play Billing
 *
 * @param environmentRepository Repository to access app environment information
 * @param getSessionTransferURLUseCase Use case to generate session transfer URLs
 */
class GeneratePurchaseUrlUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
    private val getSessionTransferURLUseCase: GetSessionTransferURLUseCase,
) {

    /**
     * Generates a purchase URL for external checkout.
     *
     * @param productId The product ID for the subscription (e.g., "propay_1", "propay_2")
     * @param months Optional billing period in months (1 for monthly, 12 for yearly, null if not specified)
     * @param externalTransactionToken Optional external transaction token from Google Play Billing
     * @return The complete URL for external purchase checkout
     */
    suspend operator fun invoke(
        productId: String,
        months: Int?,
        externalTransactionToken: String? = null,
    ): String {
        val uao = getUao()
        val queryParams = buildString {
            val params = mutableListOf<String>()
            months?.let { params.add("m=$it") }
            externalTransactionToken?.let { params.add("external_transaction_token=$it") }
            if (params.isNotEmpty()) {
                append("?${params.joinToString("&")}")
            }
        }
        return getSessionTransferURLUseCase("$productId/uao=$uao$queryParams")
    }

    /**
     * Gets the user agent override (UAO) string.
     *
     * @return A string in the format "Android app Ver {version}"
     */
    private fun getUao(): String =
        "Android app Ver ${environmentRepository.getAppInfo().appVersion}"
}
