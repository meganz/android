package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.LocalPricing

/**
 * Get local pricing for specific subscription option
 */
fun interface GetLocalPricing {
    /**
     * Invoke
     *
     * @param sku = String
     * @return LocalPricing?
     */
    suspend operator fun invoke(
        sku: String,
    ): LocalPricing?
}