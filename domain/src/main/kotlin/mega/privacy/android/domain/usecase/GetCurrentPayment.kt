package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.PaymentMethod


/**
 * Get current payment available through the store
 */
fun interface GetCurrentPayment {
    /**
     * Invoke
     *
     * @return [PaymentMethod]
     */
    suspend operator fun invoke(): PaymentMethod?
}