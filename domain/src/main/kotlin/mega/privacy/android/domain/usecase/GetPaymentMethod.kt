package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.billing.PaymentMethodFlags

/**
 * Get payment method
 *
 */
fun interface GetPaymentMethod {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(forceRefresh: Boolean): PaymentMethodFlags
}